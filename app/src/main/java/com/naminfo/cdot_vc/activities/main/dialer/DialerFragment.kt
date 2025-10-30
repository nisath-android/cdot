package com.naminfo.cdot_vc.activities.main.dialer

import android.Manifest
import android.annotation.TargetApi
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.naminfo.cdot_vc.BuildConfig
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericActivity
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.main.fragments.SecureFragment
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
import com.naminfo.cdot_vc.compatibility.Compatibility
import com.naminfo.cdot_vc.databinding.FragmentDialerBinding
import com.naminfo.cdot_vc.telecom.TelecomHelper
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.DialogUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.PermissionHelper
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import org.linphone.mediastream.Version

class DialerFragment : SecureFragment<FragmentDialerBinding>() {
    private lateinit var viewModel: DialerViewModel

    private var uploadLogsInitiatedByUs = false

    override fun getLayoutId(): Int = R.layout.fragment_dialer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[DialerViewModel::class.java]
        binding.viewModel = viewModel

        useMaterialSharedAxisXForwardAnimation = false
        checkPermissions()
        dialpadManagement()
        /*sharedViewModel.updateDialerAnimationsBasedOnDestination.observe(
            viewLifecycleOwner
        ) {
            it.consume { id ->
                val forward = when (id) {
                    R.id.masterChatRoomsFragment -> false
                    else -> true
                }
                if (corePreferences.enableAnimations) {
                    val portraitOrientation =
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    val axis =
                        if (portraitOrientation) MaterialSharedAxis.X else MaterialSharedAxis.Y
                    enterTransition = MaterialSharedAxis(axis, forward)
                    reenterTransition = MaterialSharedAxis(axis, forward)
                    returnTransition = MaterialSharedAxis(axis, !forward)
                    exitTransition = MaterialSharedAxis(axis, !forward)
                }
            }
        }*/



        viewModel.enteredUri.observe(
            viewLifecycleOwner
        ) {
            if (it == corePreferences.debugPopupCode) {
                displayDebugPopup()
                viewModel.enteredUri.value = ""
            }
        }

        viewModel.uploadFinishedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { url ->
                // To prevent being trigger when using the Send Logs button in About page
                if (uploadLogsInitiatedByUs) {
                    val clipboard =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Logs url", url)
                    clipboard.setPrimaryClip(clip)

                    val activity = requireActivity() as MainActivity
                    activity.showSnackBar(R.string.logs_url_copied_to_clipboard)

                    AppUtils.shareUploadedLogsUrl(activity, url)
                }
            }
        }

        viewModel.updateAvailableEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { url ->
                displayNewVersionAvailableDialog(url)
            }
        }

        viewModel.onMessageToNotifyEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { resourceId ->
                (requireActivity() as MainActivity).showSnackBar(resourceId)
            }
        }

        if (corePreferences.firstStart) {
            Log.i("Dialer",
                "[Dialer] First start detected, wait for assistant to be finished to check for update & request permissions"
            )
            return
        }

        if (arguments?.containsKey("URI") == true) {
            val address = arguments?.getString("URI") ?: ""
            Log.i("Dialer","[Dialer] Found URI to call: $address")
            val skipAutoCall = arguments?.getBoolean("SkipAutoCallStart") ?: false

            if (corePreferences.skipDialerForNewCallAndTransfer) {
                if (sharedViewModel.pendingCallTransfer) {
                    Log.i("Dialer",
                        "[Dialer] We were asked to skip dialer so starting new call to [$address] now"
                    )
                    viewModel.transferCallTo(address)
                } else {
                    Log.i("Dialer",
                        "[Dialer] We were asked to skip dialer so starting transfer to [$address] now"
                    )
                    viewModel.directCall(address)
                }
            } else if (corePreferences.callRightAway && !skipAutoCall) {
                Log.i("Dialer","[Dialer] Call right away setting is enabled, start the call to [$address]")
                viewModel.directCall(address)
            } else {
                sharedViewModel.dialerUri = address
            }
        }
        arguments?.clear()

        Log.i("Dialer","[Dialer] Pending call transfer mode = ${sharedViewModel.pendingCallTransfer}")
        viewModel.transferVisibility.value = sharedViewModel.pendingCallTransfer

        viewModel.autoInitiateVideoCalls.value = coreContext.core.videoActivationPolicy.automaticallyInitiate
        checkForUpdate()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
           requireActivity().finishAffinity() // closes app
            //requireActivity().finishAndRemoveTask() // closes app
        }
    }


    override fun onPause() {
        sharedViewModel.dialerUri = viewModel.enteredUri.value ?: ""
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

//        if ((requireActivity() as GenericActivity).isTablet()) {
//            coreContext.core.nativePreviewWindowId = binding.videoPreviewWindow
//        }

        viewModel.updateShowVideoPreview()
        viewModel.autoInitiateVideoCalls.value = coreContext.core.videoActivationPolicy.automaticallyInitiate
        uploadLogsInitiatedByUs = false

        viewModel.enteredUri.value = sharedViewModel.dialerUri
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Dialer","[Dialer] READ_PHONE_STATE permission has been granted")
                coreContext.initPhoneStateListener()
                // If first permission has been granted, continue to ask for permissions,
                // otherwise don't do it or it will loop indefinitely
                checkPermissions()
            }
        } else if (requestCode == 1) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                }
            }
            if (allGranted) {
                Log.i("Dialer","[Dialer] Telecom Manager permission have been granted")
                enableTelecomManager()
            } else {
                Log.w("Dialer","[Dialer] Telecom Manager permission have been denied (at least one of them)")
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Dialer","[Dialer] POST_NOTIFICATIONS permission has been granted")
            }
            checkTelecomManagerPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkPermissions() {
        if (!PermissionHelper.get().hasReadPhoneStatePermission()) {
            Log.i("Dialer","[Dialer] Asking for READ_PHONE_STATE permission")
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 0)
        } else if (!PermissionHelper.get().hasPostNotificationsPermission()) {
            // Don't check the following the previous permission is being asked
            Log.i("Dialer","[Dialer] Asking for POST_NOTIFICATIONS permission")
            Compatibility.requestPostNotificationsPermission(this, 2)
        } else if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            // Don't check the following the previous permissions are being asked
            checkTelecomManagerPermissions()
        }

        // See https://developer.android.com/about/versions/14/behavior-changes-14#fgs-types
        if (Version.sdkAboveOrEqual(Version.API34_ANDROID_14_UPSIDE_DOWN_CAKE)) {
            val fullScreenIntentPermission = Compatibility.hasFullScreenIntentPermission(
                requireContext()
            )
            Log.i("Dialer",
                "[Dialer] Android 14 or above detected: full-screen intent permission is ${if (fullScreenIntentPermission) "granted" else "not granted"}"
            )
            if (!fullScreenIntentPermission) {
                (requireActivity() as MainActivity).showSnackBar(
                    R.string.android_14_full_screen_intent_permission_not_granted,
                    R.string.android_14_go_to_full_screen_intent_permission_setting
                ) {
                    Compatibility.requestFullScreenIntentPermission(requireContext())
                }
            }
        }
    }

    @TargetApi(Version.API26_O_80)
    private fun checkTelecomManagerPermissions() {
        if (!corePreferences.useTelecomManager) {
            Log.i("Dialer","[Dialer] Telecom Manager feature is disabled")
            if (corePreferences.manuallyDisabledTelecomManager) {
                Log.w("Dialer","[Dialer] User has manually disabled Telecom Manager feature")
            } else {
                if (Compatibility.hasTelecomManagerPermissions(requireContext())) {
                    enableTelecomManager()
                } else {
                    Log.i("Dialer","[Dialer] Asking for Telecom Manager permissions")
                    Compatibility.requestTelecomManagerPermissions(requireActivity(), 1)
                }
            }
        } else {
            Log.i("Dialer","[Dialer] Telecom Manager feature is already enabled")
        }
    }

    @TargetApi(Version.API26_O_80)
    private fun enableTelecomManager() {
        Log.i("Dialer","[Dialer] Telecom Manager permissions granted")
        if (!TelecomHelper.exists()) {
            Log.i("Dialer","[Dialer] Creating Telecom Helper")
            if (Compatibility.hasTelecomManagerFeature(requireContext())) {
                TelecomHelper.create(requireContext())
            } else {
                Log.e("Dialer",
                    "[Dialer] Telecom Helper can't be created, device doesn't support connection service!"
                )
                return
            }
        } else {
            Log.e("Dialer","[Dialer] Telecom Manager was already created ?!")
        }
        corePreferences.useTelecomManager = true
    }

    private fun displayDebugPopup() {
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
        alertDialog.setTitle(getString(R.string.debug_popup_title))

        val items = if (corePreferences.debugLogs) {
            resources.getStringArray(R.array.popup_send_log)
        } else {
            resources.getStringArray(R.array.popup_enable_log)
        }

        alertDialog.setItems(items) { _, which ->
            when (items[which]) {
                getString(R.string.debug_popup_disable_logs) -> {
                    corePreferences.debugLogs = false
                }
                getString(R.string.debug_popup_enable_logs) -> {
                    corePreferences.debugLogs = true
                }
                getString(R.string.debug_popup_send_logs) -> {
                    uploadLogsInitiatedByUs = true
                    viewModel.uploadLogs()
                }
                getString(R.string.debug_popup_show_config_file) -> {
                    //navigateToConfigFileViewer()
                }
            }
        }

        alertDialog.show()
    }

    private fun checkForUpdate() {
        val lastTimestamp: Int = corePreferences.lastUpdateAvailableCheckTimestamp
        val currentTimeStamp = System.currentTimeMillis().toInt()
        val interval: Int = corePreferences.checkUpdateAvailableInterval
        if (lastTimestamp == 0 || currentTimeStamp - lastTimestamp >= interval) {
            val currentVersion = BuildConfig.VERSION_NAME
            Log.i("Dialer","[Dialer] Checking for update using current version [$currentVersion]")
            coreContext.core.checkForUpdate(currentVersion)
            corePreferences.lastUpdateAvailableCheckTimestamp = currentTimeStamp
        }
    }

    private fun displayNewVersionAvailableDialog(url: String) {
        val viewModel = DialogViewModel(getString(R.string.dialog_update_available))
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), viewModel)

        viewModel.showCancelButton {
            dialog.dismiss()
        }

        viewModel.showOkButton(
            {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } catch (ise: IllegalStateException) {
                    Log.e("Dialer","[Dialer] Can't start ACTION_VIEW intent, IllegalStateException: $ise")
                } finally {
                    dialog.dismiss()
                }
            },
            getString(R.string.dialog_ok)
        )

        dialog.show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.sipUriInput.windowToken, 0)
    }
    private fun hideDialPad() {
        // Example: if you have a layout for dialpad
        binding.numpad.root.visibility = View.VISIBLE
    }
    fun dialpadManagement() {
        val editText = binding.sipUriInput
        // Disable keyboard popup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.showSoftInputOnFocus = false
        } else {
            editText.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.performClick() // ✅ Accessibility safe
                   hideKeyboard()
                }
                true
            }
        }

        // Show cursor but not keyboard
        editText.isCursorVisible = true

        // Handle touch for hiding dialpad/keyboard
        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.performClick() // ✅ Required for accessibility
                hideDialPad()
                hideKeyboard()
            }
            false
        }
    }




}
