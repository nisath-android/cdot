package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.CallSettingsViewModel
import com.naminfo.cdot_vc.compatibility.Compatibility
import com.naminfo.cdot_vc.databinding.FragmentCallSettingsBinding
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version
import com.naminfo.cdot_vc.telecom.TelecomHelper
import kotlin.text.get

class CallSettingsFragment : GenericSettingFragment<FragmentCallSettingsBinding>() {
    private lateinit var viewModel: CallSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_call_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[CallSettingsViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.systemWideOverlayEnabledEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (!Compatibility.canDrawOverlay(requireContext())) {
                    val intent = Intent(
                        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivityForResult(intent, 0)
                }
            }
        }

        viewModel.goToAndroidNotificationSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
                    val i = Intent()
                    i.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    i.putExtra(
                        Settings.EXTRA_CHANNEL_ID,
                        getString(R.string.notification_channel_service_id)
                    )
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(i)
                }
            }
        }

        viewModel.enableTelecomManagerEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Compatibility.hasTelecomManagerFeature(requireContext())) {
                    if (!Compatibility.hasTelecomManagerPermissions(requireContext())) {
                        Compatibility.requestTelecomManagerPermissions(requireActivity(), 1)
                    } else if (!TelecomHelper.exists()) {
                        corePreferences.useTelecomManager = true
                        Log.w("[Telecom Helper] Doesn't exists yet, creating it")
                        TelecomHelper.create(requireContext())
                        updateTelecomManagerAccount()
                    }
                } else {
                    Log.e(
                        "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service!"
                    )
                }
            }
        }

        viewModel.goToAndroidNotificationSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
                    val i = Intent()
                    i.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    i.putExtra(
                        Settings.EXTRA_CHANNEL_ID,
                        getString(R.string.notification_channel_service_id)
                    )
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(i)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && !Compatibility.canDrawOverlay(requireContext())) {
            viewModel.overlayListener.onBoolValueChanged(false)
        } else if (requestCode == 1) {
            if (!TelecomHelper.exists()) {
                Log.w("[Telecom Helper] Doesn't exists yet, creating it")
                if (Compatibility.hasTelecomManagerFeature(requireContext())) {
                    TelecomHelper.create(requireContext())
                } else {
                    Log.e(
                        "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service"
                    )
                }
            }
            updateTelecomManagerAccount()
        }
    }

    private fun updateTelecomManagerAccount() {
        if (!TelecomHelper.exists()) {
            Log.e("[Telecom Helper] Doesn't exists, can't update account!")
            return
        }
        // We have to refresh the account object otherwise isAccountEnabled will always return false...
        val account = TelecomHelper.get().findExistingAccount(requireContext())
        TelecomHelper.get().updateAccount(account)
        val enabled = TelecomHelper.get().isAccountEnabled()
        Log.i("[Call Settings] Telecom Manager is ${if (enabled) "enabled" else "disabled"}")
        viewModel.useTelecomManager.value = enabled
        corePreferences.useTelecomManager = enabled
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (index in grantResults.indices) {
            val result = grantResults[index]
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.w(
                    "[Call Settings] ${permissions[index]} permission denied but required for telecom manager"
                )
                viewModel.useTelecomManager.value = false
                corePreferences.useTelecomManager = false
                return
            }
        }

        if (Compatibility.hasTelecomManagerFeature(requireContext())) {
            TelecomHelper.create(requireContext())
            updateTelecomManagerAccount()
        } else {
            Log.e(
                "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service"
            )
        }
    }
}



/*class CallSettingsFragment : GenericSettingFragment<FragmentCallSettingsBinding>() {
    private lateinit var viewModel: CallSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_call_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[CallSettingsViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.systemWideOverlayEnabledEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (!Compatibility.canDrawOverlay(requireContext())) {
                    val intent = Intent(
                        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivityForResult(intent, 0)
                }
            }
        }

        viewModel.goToAndroidNotificationSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
                    val i = Intent()
                    i.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    i.putExtra(
                        Settings.EXTRA_CHANNEL_ID,
                        getString(R.string.notification_channel_service_id)
                    )
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(i)
                }
            }
        }

        viewModel.enableTelecomManagerEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Compatibility.hasTelecomManagerFeature(requireContext())) {
                    if (!Compatibility.hasTelecomManagerPermissions(requireContext())) {
                        Compatibility.requestTelecomManagerPermissions(requireActivity(), 1)
                    } else if (!TelecomHelper.exists()) {
                        corePreferences.useTelecomManager = true
                        Log.w("[Telecom Helper] Doesn't exists yet, creating it")
                        TelecomHelper.create(requireContext())
                        updateTelecomManagerAccount()
                    }
                } else {
                    Log.e(
                        "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service!"
                    )
                }
            }
        }

        viewModel.goToAndroidNotificationSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                if (Build.VERSION.SDK_INT >= Version.API26_O_80) {
                    val i = Intent()
                    i.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    i.putExtra(
                        Settings.EXTRA_CHANNEL_ID,
                        getString(R.string.notification_channel_service_id)
                    )
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(i)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && !Compatibility.canDrawOverlay(requireContext())) {
            viewModel.overlayListener.onBoolValueChanged(false)
        } else if (requestCode == 1) {
            if (!TelecomHelper.exists()) {
                Log.w("[Telecom Helper] Doesn't exists yet, creating it")
                if (Compatibility.hasTelecomManagerFeature(requireContext())) {
                    TelecomHelper.create(requireContext())
                } else {
                    Log.e(
                        "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service"
                    )
                }
            }
            updateTelecomManagerAccount()
        }
    }

    private fun updateTelecomManagerAccount() {
        if (!TelecomHelper.exists()) {
            Log.e("[Telecom Helper] Doesn't exists, can't update account!")
            return
        }
        // We have to refresh the account object otherwise isAccountEnabled will always return false...
        val account = TelecomHelper.get().findExistingAccount(requireContext())
        TelecomHelper.get().updateAccount(account)
        val enabled = TelecomHelper.get().isAccountEnabled()
        Log.i("[Call Settings] Telecom Manager is ${if (enabled) "enabled" else "disabled"}")
        viewModel.useTelecomManager.value = enabled
        corePreferences.useTelecomManager = enabled
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (index in grantResults.indices) {
            val result = grantResults[index]
            if (result != PackageManager.PERMISSION_GRANTED) {
                Log.w(
                    "[Call Settings] ${permissions[index]} permission denied but required for telecom manager"
                )
                viewModel.useTelecomManager.value = false
                corePreferences.useTelecomManager = false
                return
            }
        }

        if (Compatibility.hasTelecomManagerFeature(requireContext())) {
            TelecomHelper.create(requireContext())
            updateTelecomManagerAccount()
        } else {
            Log.e(
                "[Telecom Helper] Telecom Helper can't be created, device doesn't support connection service"
            )
        }
    }
}*/