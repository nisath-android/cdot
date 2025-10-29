package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.BR
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AudioSettingsViewModel
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.databinding.FragmentAudioSettingsBinding
import com.naminfo.cdot_vc.utils.PermissionHelper

class AudioSettingsFragment : GenericSettingFragment<FragmentAudioSettingsBinding>() {
    private lateinit var viewModel: AudioSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_audio_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[AudioSettingsViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.askAudioRecordPermissionForEchoCancellerCalibrationEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                Log.i(
                    "[Audio Settings] Asking for RECORD_AUDIO permission for echo canceller calibration"
                )
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
            }
        }

        viewModel.askAudioRecordPermissionForEchoTesterEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                Log.i("[Audio Settings] Asking for RECORD_AUDIO permission for echo tester")
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 2)
            }
        }

        initAudioCodecsList()

        if (!PermissionHelper.required(requireContext()).hasRecordAudioPermission()) {
            Log.i("[Audio Settings] Asking for RECORD_AUDIO permission")
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Log.i("[Audio Settings] RECORD_AUDIO permission granted")
            if (requestCode == 1) {
                viewModel.startEchoCancellerCalibration()
            } else if (requestCode == 2) {
                viewModel.startEchoTester()
            }
        } else {
            Log.w("[Audio Settings] RECORD_AUDIO permission denied")
        }
    }

    private fun initAudioCodecsList() {
        val list = arrayListOf<ViewDataBinding>()
        for (payload in coreContext.core.audioPayloadTypes) {
            val binding = DataBindingUtil.inflate<ViewDataBinding>(
                LayoutInflater.from(requireContext()),
                R.layout.settings_widget_switch,
                null,
                false
            )
            binding.setVariable(BR.title, payload.mimeType)
            binding.setVariable(BR.subtitle, "${payload.clockRate} Hz")
            //binding.setVariable(BR.checked, payload.enabled())
            binding.setVariable(
                BR.listener,
                object : SettingListenerStub() {
                    override fun onBoolValueChanged(newValue: Boolean) {
                        payload.enable(newValue)
                    }
                }
            )
            binding.lifecycleOwner = viewLifecycleOwner
            list.add(binding)
        }
        viewModel.audioCodecs.value = list
    }
}