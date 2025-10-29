package com.naminfo.cdot_vc.activities.main.settings.viewmodels

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import java.lang.NumberFormatException
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub
import org.linphone.core.Factory
import org.linphone.core.tools.Log

class VideoSettingsViewModel : GenericSettingsViewModel() {
    val enableVideoListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            core.isVideoCaptureEnabled = true
            core.isVideoDisplayEnabled = true
            if (!newValue) {
                tabletPreview.value = false
                initiateCall.value = true
                autoAccept.value = true
            }
        }
    }
    val enableVideo = MutableLiveData<Boolean>()

    val tabletPreviewListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            prefs.videoPreview = newValue
        }
    }
    val tabletPreview = MutableLiveData<Boolean>()
    val isTablet = MutableLiveData<Boolean>()

    val initiateCallListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            val policy = core.videoActivationPolicy
            policy.automaticallyInitiate = true
            core.videoActivationPolicy = policy
        }
    }
    val initiateCall = MutableLiveData<Boolean>()

    val autoAcceptListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            val policy = core.videoActivationPolicy
            policy.automaticallyAccept = true
            core.videoActivationPolicy = policy
        }
    }
    val autoAccept = MutableLiveData<Boolean>()

    val cameraDeviceListener = object : SettingListenerStub() {
        override fun onListValueChanged(position: Int) {
            core.videoDevice = cameraDeviceLabels.value.orEmpty()[position]
        }
    }
    val cameraDeviceIndex = MutableLiveData<Int>()
    val cameraDeviceLabels = MutableLiveData<ArrayList<String>>()

    val videoSizeListener = object : SettingListenerStub() {
        override fun onListValueChanged(position: Int) {
            core.setPreferredVideoDefinitionByName(videoSizeLabels.value.orEmpty()[position])
        }
    }
    val videoSizeIndex = MutableLiveData<Int>()
    val videoSizeLabels = MutableLiveData<ArrayList<String>>()

    val videoPresetListener = object : SettingListenerStub() {
        override fun onListValueChanged(position: Int) {
            videoPresetIndex.value = position // Needed to display/hide two below settings
            val currentPreset = core.videoPreset
            val newPreset = videoPresetLabels.value.orEmpty()[position]
            if (newPreset != currentPreset) {
                if (currentPreset == "custom") {
                    // Not "custom" anymore, reset FPS & bandwidth
                    core.preferredFramerate = 0f
                    core.downloadBandwidth = 0
                    core.uploadBandwidth = 0
                }
                core.videoPreset = newPreset
            }
        }
    }
    val videoPresetIndex = MutableLiveData<Int>()
    val videoPresetLabels = MutableLiveData<ArrayList<String>>()

    val preferredFpsListener = object : SettingListenerStub() {
        override fun onListValueChanged(position: Int) {
            core.preferredFramerate = preferredFpsLabels.value.orEmpty()[position].toFloat()
        }
    }
    val preferredFpsIndex = MutableLiveData<Int>()
    val preferredFpsLabels = MutableLiveData<ArrayList<String>>()

    val bandwidthLimitListener = object : SettingListenerStub() {
        override fun onTextValueChanged(newValue: String) {
            try {
                core.downloadBandwidth = newValue.toInt()
                core.uploadBandwidth = newValue.toInt()
            } catch (_: NumberFormatException) {
            }
        }
    }
    val bandwidthLimit = MutableLiveData<Int>()

    val videoCodecs = MutableLiveData<ArrayList<ViewDataBinding>>()

    init {
        if (core.videoSupported()) {
            enableVideo.value = true
            core.isVideoCaptureEnabled = true
            core.isVideoDisplayEnabled = true
            updateVideoActivationPolicy(true) // Ensures the policy is applied
        }
        tabletPreview.value = prefs.videoPreview
        isTablet.value = coreContext.context.resources.getBoolean(R.bool.isTablet)
        initiateCall.value = true
        autoAccept.value = true

        initCameraDevicesList()
        initVideoSizeList()
        initVideoPresetList()
        initFpsList()

        bandwidthLimit.value = core.downloadBandwidth
    }

    fun initCameraDevicesList() {
        val labels = arrayListOf<String>()
        for (camera in core.videoDevicesList) {
            if (prefs.hideStaticImageCamera && camera.startsWith("StaticImage")) {
                Log.w("[Video Settings] Do not display StaticImage camera")
            } else {
                labels.add(camera)
            }
        }

        cameraDeviceLabels.value = labels
        val index = labels.indexOf(core.videoDevice)
        if (index == -1) {
            val firstDevice = cameraDeviceLabels.value.orEmpty().firstOrNull()
            Log.w(
                "[Video Settings] Device not found in labels list: ${core.videoDevice}, replace it by $firstDevice"
            )
            if (firstDevice != null) {
                cameraDeviceIndex.value = 0
                core.videoDevice = firstDevice
            }
        } else {
            cameraDeviceIndex.value = index
        }
    }

    private fun initVideoSizeList() {
        val labels = arrayListOf<String>()

        for (size in Factory.instance().supportedVideoDefinitions) {
            labels.add(size.name.orEmpty())
        }

        videoSizeLabels.value = labels
        videoSizeIndex.value = labels.indexOf(core.preferredVideoDefinition.name)
    }

    private fun initVideoPresetList() {
        val labels = arrayListOf<String>()

        labels.add("default")
        labels.add("high-fps")
        labels.add("custom")

        videoPresetLabels.value = labels
        videoPresetIndex.value = labels.indexOf(core.videoPreset)
    }

    private fun initFpsList() {
        val labels = arrayListOf("5", "10", "15", "20", "25", "30")
        preferredFpsLabels.value = labels
        preferredFpsIndex.value = labels.indexOf(core.preferredFramerate.toInt().toString())
    }

    private fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        core.videoActivationPolicy = policy
    }
}