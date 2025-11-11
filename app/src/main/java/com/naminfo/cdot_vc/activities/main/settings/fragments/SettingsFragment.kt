package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.core.view.doOnPreDraw
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.*
import com.naminfo.cdot_vc.activities.main.fragments.MasterFragment
import com.naminfo.cdot_vc.activities.main.fragments.SecureFragment
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModel
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.SettingsViewModel
import com.naminfo.cdot_vc.activities.navigateToAccountSettings
//import com.naminfo.cdot_vc.activities.navigateToAudioSettings
//import com.naminfo.cdot_vc.activities.navigateToTunnelSettings
//import com.naminfo.cdot_vc.activities.navigateToVideoSettings
import com.naminfo.cdot_vc.databinding.FragmentSettingsBinding
import org.linphone.core.tools.Log

class SettingsFragment : SecureFragment<FragmentSettingsBinding>() {
    private lateinit var viewModel: SettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        useMaterialSharedAxisXForwardAnimation = false
        if (corePreferences.enableAnimations) {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        }

        /* Shared view model & sliding pane related */

        view.doOnPreDraw { sharedViewModel.isSlidingPaneSlideable.value = binding.slidingPane.isSlideable }

        // Account settings loading can take some time, so wait until it is ready before opening the pane
        sharedViewModel.accountSettingsFragmentOpenedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                binding.slidingPane.openPane()
            }
        }


        sharedViewModel.layoutChangedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                sharedViewModel.isSlidingPaneSlideable.value = binding.slidingPane.isSlideable
                if (binding.slidingPane.isSlideable) {
                    val navHostFragment =
                        childFragmentManager.findFragmentById(R.id.settings_nav_container) as NavHostFragment
                    if (navHostFragment.navController.currentDestination?.id == R.id.emptySettingsFragment) {
                        Log.i(
                            "[Settings] Foldable device has been folded, closing side pane with empty fragment"
                        )
                        binding.slidingPane.closePane()
                    }
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            MasterFragment.SlidingPaneBackPressedCallback(binding.slidingPane)
        )

        binding.slidingPane.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

        /* End of shared view model & sliding pane related */

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        binding.viewModel = viewModel

        sharedViewModel.accountRemoved.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Settings] Account removed, update accounts list")
            viewModel.updateAccountsList()
        }

        sharedViewModel.defaultAccountChanged.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Settings] Default account changed, update accounts list")
            viewModel.updateAccountsList()
        }

        val identity = arguments?.getString("Identity")
        if (identity != null) {
            Log.i("[Settings] Found identity parameter in arguments: $identity")
            arguments?.clear()
            navigateToAccountSettings(identity)
        }
        binding.back.setOnClickListener {
            //goBack()
          /*  if (identity != null) {
                viewModel.accountsSettingsListener.onAccountClicked(identity)
            }*/
            navigateToDialer()
        }
        viewModel.accountsSettingsListener = object : SettingListenerStub() {
            override fun onAccountClicked(identity: String) {
                Log.i("[Settings] Navigation to settings for account with identity: $identity")
                navigateToAccountSettings(identity)
            }
        }

        viewModel.tunnelSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                //navigateToTunnelSettings(binding.slidingPane)
            }
        }

        viewModel.audioSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                navigateToAudioSettings(binding.slidingPane)
            }
        }

        viewModel.videoSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                navigateToVideoSettings(binding.slidingPane)
            }
        }

        viewModel.callSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                navigateToCallSettings(binding.slidingPane)
            }
        }

        viewModel.chatSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                //navigateToChatSettings(binding.slidingPane)
            }
        }

        viewModel.networkSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                navigateToNetworkSettings(binding.slidingPane)
            }
        }

        viewModel.contactsSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                //navigateToContactsSettings(binding.slidingPane)
            }
        }

        viewModel.advancedSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                navigateToAdvancedSettings(binding.slidingPane)
            }
        }

        viewModel.conferencesSettingsListener = object : SettingListenerStub() {
            override fun onClicked() {
                //navigateToConferencesSettings(binding.slidingPane)
            }
        }

    }

    internal fun SettingsFragment.navigateToDialer() {
        /* val action = when (findNavController().currentDestination?.id) {
             R.id.masterContactsFragment -> R.id.action_masterContactsFragment_to_dialerFragment
             R.id.accountSettingsFragment -> R.id.action_settingsFragment_to_dialerFragment
             else -> R.id.action_global_dialerFragment
         }
         findNavController().navigate(
             action,
             null,
             popupTo(R.id.dialerFragment, true)
         )*/
       /* findNavController().navigate(
            R.id.action_global_accountSettingsFragment
        )*/
        findNavController().navigate(
            R.id.action_global_dialerFragment,
            null,

        )
    }
}