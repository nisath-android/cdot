package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AdvancedSettingsViewModel
import com.naminfo.cdot_vc.databinding.FragmentAdvancedSettingsBinding
import org.linphone.core.tools.Log
import org.linphone.core.tools.compatibility.DeviceUtils
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.PowerManagerUtils

class AdvancedSettingsFragment : GenericSettingFragment<FragmentAdvancedSettingsBinding>() {
    private lateinit var viewModel: AdvancedSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_advanced_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[AdvancedSettingsViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.uploadFinishedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { url ->
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Logs url", url)
                clipboard.setPrimaryClip(clip)

                val activity = requireActivity() as MainActivity
                activity.showSnackBar(R.string.logs_url_copied_to_clipboard)

                AppUtils.shareUploadedLogsUrl(activity, url)
            }
        }

        viewModel.uploadErrorEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val activity = requireActivity() as MainActivity
                activity.showSnackBar(R.string.logs_upload_failure)
            }
        }

        viewModel.resetCompleteEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val activity = requireActivity() as MainActivity
                activity.showSnackBar(R.string.logs_reset_complete)
            }
        }

        viewModel.setNightModeEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { value ->
                AppCompatDelegate.setDefaultNightMode(
                    when (value) {
                        0 -> AppCompatDelegate.MODE_NIGHT_NO
                        1 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }
        }

        viewModel.backgroundModeEnabled.value = !DeviceUtils.isAppUserRestricted(requireContext())

        viewModel.goToBatterySettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                try {
                    val intent = Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
                    startActivity(intent)
                } catch (anfe: ActivityNotFoundException) {
                    Log.e("[Advanced Settings] ActivityNotFound exception: ", anfe)
                }
            }
        }

        viewModel.powerManagerSettingsVisibility.value = PowerManagerUtils.getDevicePowerManagerIntent(
            requireContext()
        ) != null
        viewModel.goToPowerManagerSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val intent = PowerManagerUtils.getDevicePowerManagerIntent(requireActivity())
                if (intent != null) {
                    try {
                        startActivity(intent)
                    } catch (se: SecurityException) {
                        Log.e("[Advanced Settings] Security exception: ", se)
                    }
                }
            }
        }

        viewModel.goToAndroidSettingsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                ContextCompat.startActivity(requireContext(), intent, null)
            }
        }
    }
}
