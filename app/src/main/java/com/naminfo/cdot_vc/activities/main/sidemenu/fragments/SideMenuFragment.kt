package com.naminfo.cdot_vc.activities.main.sidemenu.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.*
import com.naminfo.cdot_vc.activities.assistant.AssistantActivity
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModel
import com.naminfo.cdot_vc.activities.main.sidemenu.viewmodels.SideMenuViewModel
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
import org.linphone.core.Factory
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.databinding.SideMenuFragmentBinding
import com.naminfo.cdot_vc.utils.*

class SideMenuFragment : GenericFragment<SideMenuFragmentBinding>() {
    private lateinit var viewModel: SideMenuViewModel
    private var temporaryPicturePath: File? = null

    override fun getLayoutId(): Int = R.layout.side_menu_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        // sharedViewModel = ViewModelProvider(this)[SharedMainViewModel::class.java]
        viewModel = ViewModelProvider(this)[SideMenuViewModel::class.java]
        binding.viewModel = viewModel
        MainActivity.sideMenuFragment = this
        /*if (viewModel.defaultAccountFound.value == true) {
            binding.loginText.visibility = View.GONE
        } else {
            binding.loginText.visibility = View.VISIBLE
        }*/
        viewModel.defaultAccountViewModel.value?.displayName?.value =
            sharedViewModel.getCurrentUserName()
        binding.userNameTV.setText(sharedViewModel.getCurrentUserName())
        viewModel.defaultAccountFound.observe(viewLifecycleOwner) { isFound ->
            binding.loginText.visibility = if (isFound) View.GONE else View.VISIBLE
        }

        sharedViewModel.accountRemoved.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Side Menu] Account removed, update accounts list")
            viewModel.updateAccountsList()
        }

        sharedViewModel.defaultAccountChanged.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Side Menu] Default account changed, update accounts list")
            viewModel.updateAccountsList()
        }
        viewModel.updated.observe(viewLifecycleOwner) {
            lifecycleScope.launch(Dispatchers.Main) {
                //  Log.i("[Side Menu]", "...........$it")
                val defaultAccount = coreContext.core.defaultAccount
                if (defaultAccount != null) {
                    Log.i("[Side Menu]", "...........$it")
                    val defaultViewModel = AccountSettingsViewModel(defaultAccount)
                    defaultViewModel.displayName.value = it
                    binding.userNameTV.setText(it)
                }
                binding.userNameTV.setText(it)
            }
        }
        sharedViewModel.publishPresenceToggled.observe(
            viewLifecycleOwner
        ) {
            viewModel.refreshConsolidatedPresence()
        }

        viewModel.accountsSettingsListener = object : SettingListenerStub() {
            override fun onAccountClicked(identity: String) {
                Log.i("[Side Menu] Navigating to settings for account with identity: $identity")

                sharedViewModel.toggleDrawerEvent.value = Event(true)

                if (corePreferences.askForAccountPasswordToAccessSettings) {
                    showPasswordDialog(goToAccountSettings = true, accountIdentity = identity)
                } else {
                    navigateToAccountSettings(identity)
                }
            }
        }

        binding.setSelfPictureClickListener {
            pickFile()
        }

        binding.setAssistantClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            startActivity(Intent(context, AssistantActivity::class.java))
        }

        binding.setSettingsClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)

            if (corePreferences.askForAccountPasswordToAccessSettings) {
                showPasswordDialog(goToSettings = true)
            } else {
                navigateToSettings()
            }
        }

        binding.setRecordingsClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            //navigateToRecordings()
        }

        binding.setAboutClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            navigateToAbout()
        }

        binding.setConferencesClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
            //navigateToScheduledConferences()
        }

        binding.setQuitClickListener {
            val context = requireContext()

            // Create the AlertDialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Quit CDOT_VC")
            builder.setMessage(
                "If you quit the CDOT_VC, you may not receive calls from the app."
            )

            // OK button action
            builder.setPositiveButton("OK") { dialog, _ ->
                Log.i("[Side Menu] Quitting app")
                requireActivity().finishAndRemoveTask()

                // Log.i("[Side Menu] Stopping Core Context")
                // coreContext.notificationsManager.stopForegroundNotification()
                // coreContext.stop()
                dialog.dismiss()
            }

            // Cancel button action
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog without quitting the app
            }

            // Display the dialog
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                val contactImageFilePath = FileUtils.getFilePathFromPickerIntent(
                    data,
                    temporaryPicturePath
                )
                if (contactImageFilePath != null) {
                    viewModel.setPictureFromPath(contactImageFilePath)
                }
            }
        }
    }

    private fun pickFile() {
        val cameraIntents = ArrayList<Intent>()

        // Handles image picking
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"

        if (PermissionHelper.get().hasCameraPermission()) {
            // Allows to capture directly from the camera
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val tempFileName = System.currentTimeMillis().toString() + ".jpeg"
            val file = FileUtils.getFileStoragePath(tempFileName)
            temporaryPicturePath = file
            /*val publicUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getString(R.string.file_provider),
                file
            )*/
            //captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, publicUri)
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cameraIntents.add(captureIntent)
        }

        val chooserIntent =
            Intent.createChooser(galleryIntent, getString(R.string.chat_message_pick_file_dialog))
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            cameraIntents.toArray(arrayOf<Parcelable>())
        )

        startActivityForResult(chooserIntent, 0)
    }

    private fun showPasswordDialog(
        goToSettings: Boolean = false,
        goToAccountSettings: Boolean = false,
        accountIdentity: String = ""
    ) {
        val dialogViewModel = DialogViewModel(
            getString(R.string.settings_password_protection_dialog_title)
        )
        dialogViewModel.showIcon = true
        dialogViewModel.iconResource = R.drawable.security_toggle_icon_green
        dialogViewModel.showPassword = true
        dialogViewModel.passwordTitle = getString(
            R.string.settings_password_protection_dialog_input_hint
        )
        val dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialogViewModel.showOkButton(
            {
                val defaultAccount =
                    coreContext.core.defaultAccount ?: coreContext.core.accountList.firstOrNull()
                if (defaultAccount == null) {
                    Log.e("[Side Menu] No account found, can't check password input!")
                    (requireActivity() as MainActivity).showSnackBar(R.string.error_unexpected)
                } else {
                    val authInfo = defaultAccount.findAuthInfo()
                    if (authInfo == null) {
                        Log.e(
                            "[Side Menu] No auth info found for account [${defaultAccount.params.identityAddress?.asString()}], can't check password input!"
                        )
                        (requireActivity() as MainActivity).showSnackBar(R.string.error_unexpected)
                    } else {
                        val expectedHash = authInfo.ha1
                        if (expectedHash == null) {
                            Log.e(
                                "[Side Menu] No ha1 found in auth info, can't check password input!"
                            )
                            (requireActivity() as MainActivity).showSnackBar(
                                R.string.error_unexpected
                            )
                        } else {
                            val hashAlgorithm = authInfo.algorithm ?: "MD5"
                            val userId = (authInfo.userid ?: authInfo.username).orEmpty()
                            val realm = authInfo.realm.orEmpty()
                            val password = dialogViewModel.password
                            val computedHash = Factory.instance().computeHa1ForAlgorithm(
                                userId,
                                password,
                                realm,
                                hashAlgorithm
                            )
                            if (computedHash != expectedHash) {
                                Log.e(
                                    "[Side Menu] Computed hash [$computedHash] using userId [$userId], realm [$realm] and algorithm [$hashAlgorithm] doesn't match expected hash!"
                                )
                                (requireActivity() as MainActivity).showSnackBar(
                                    R.string.settings_password_protection_dialog_invalid_input
                                )
                            } else {
                                if (goToSettings) {
                                    navigateToSettings()
                                } else if (goToAccountSettings) {
                                    navigateToAccountSettings(accountIdentity)
                                }
                            }
                        }
                    }
                }

                dialog.dismiss()
            },
            getString(R.string.settings_password_protection_dialog_ok_label)
        )

        dialog.show()
    }

    fun isDrawerClosed(b: Boolean) {
        val defaultAccount = coreContext.core.defaultAccount
        if (defaultAccount != null) {
            Log.i("[Side Menu]", "isDrawerClosed=$b...........$corePreferences.getCurrentUserName")
            val defaultViewModel = AccountSettingsViewModel(defaultAccount)
            defaultViewModel.displayName.value = corePreferences.getCurrentUserName
            binding.userNameTV.setText(corePreferences.getCurrentUserName)
        }
        binding.userNameTV.setText(corePreferences.getCurrentUserName)
    }
}
