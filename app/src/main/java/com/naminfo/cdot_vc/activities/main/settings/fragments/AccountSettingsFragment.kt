package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModel
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModelFactory
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
//import com.naminfo.cdot_vc.activities.navigateToPhoneLinking
import com.naminfo.cdot_vc.databinding.FragmentAccountSettingsBinding
import com.naminfo.cdot_vc.utils.AppUtils
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.utils.DialogUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.LinphoneUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccountSettingsFragment : GenericSettingFragment<FragmentAccountSettingsBinding>() {
    private lateinit var viewModel: AccountSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_account_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        val identity = arguments?.getString("Identity")
        if (identity == null) {
            Log.e("[Account Settings] Identity is null, aborting!")
            goBack()
            return
        }

        try {
            viewModel = ViewModelProvider(this, AccountSettingsViewModelFactory(identity))[AccountSettingsViewModel::class.java]
        } catch (nsee: NoSuchElementException) {
            Log.e("[Account Settings] Failed to find Account object, aborting!")
            goBack()
            return
        }
        binding.viewModel = viewModel
        val colorInt = requireContext()?.let { ContextCompat.getColor(it, R.color.test) }
        val colorString = String.format("#%06X", 0xFFFFFF and colorInt!!)
        viewModel.audioCardColor.value = colorString ?:"#fa8072"

        Log.e("[Account Settings] ${viewModel.displayUsernameInsteadOfIdentity}|| ${viewModel.displayName.value} || ${viewModel.identity.value}")
        viewModel.linkPhoneNumberEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val authInfo = viewModel.account.findAuthInfo()
                if (authInfo == null) {
                    Log.e(
                        "[Account Settings] Failed to find auth info for account ${viewModel.account}"
                    )
                } else {
                    val args = Bundle()
                    args.putString("Username", authInfo.username)
                    args.putString("Password", authInfo.password)
                    args.putString("HA1", authInfo.ha1)
                    //navigateToPhoneLinking(args)
                }
            }
        }

        viewModel.accountRemovedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                sharedViewModel.accountRemoved.value = true
                LinphoneApplication.coreContext.notificationsManager.stopForegroundNotification()
                Log.i("[Side Menu] Stopping Core Context")
               LinphoneUtils.restartAppMyPid(requireActivity())
             /*   lifecycleScope.launch {
                    delay(2000)
                }
                requireActivity().finishAndRemoveTask()*/
                Log.i("[Side Menu] Quitting app")
              /*  lifecycleScope.launch {
                    delay(5000)
                }*/

                LinphoneApplication.coreContext.stop()
                // goBack()



            }
        }

        viewModel.accountDefaultEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                sharedViewModel.defaultAccountChanged.value = true
            }
        }

        viewModel.deleteAccountRequiredEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val defaultDomainAccount = viewModel.account.params.identityAddress?.domain == corePreferences.defaultDomain
                Log.i(
                    "[Account Settings] User clicked on delete account, showing confirmation dialog for ${if (defaultDomainAccount) "default domain account" else "third party account"}"
                )
                val dialogViewModel = if (defaultDomainAccount) {
                    DialogViewModel(
                        getString(
                            R.string.account_setting_delete_sip_linphone_org_confirmation_dialog
                        ),
                        getString(R.string.account_setting_delete_dialog_title)
                    )
                } else {
                    DialogViewModel(
                        getString(R.string.account_setting_delete_generic_confirmation_dialog),
                        getString(R.string.account_setting_delete_dialog_title)
                    )
                }
                val dialog: Dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

                dialogViewModel.showIcon = true
                dialogViewModel.iconResource = R.drawable.dialog_delete_icon
                dialogViewModel.showSubscribeLinphoneOrgLink = defaultDomainAccount

                dialogViewModel.showCancelButton {
                    Log.i("[Account Settings] User cancelled account removal")
                    dialog.dismiss()
                }

                dialogViewModel.showDeleteButton(
                    {
                        val dbPath = context?.getDatabasePath("call-history.db")
                        Log.i("[Account Settings] DB path : $dbPath")
                        if (dbPath != null) {
                            dbPath.delete()
                        }
                        viewModel.startDeleteAccount()
                        dialog.dismiss()
                    },
                    getString(R.string.dialog_delete)
                )

                dialog.show()
            }
        }

        view.doOnPreDraw {
            // Notifies fragment is ready to be drawn
            sharedViewModel.accountSettingsFragmentOpenedEvent.value = Event(true)
        }
    }
}
