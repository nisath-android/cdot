@file:Suppress("DEPRECATION")
package com.naminfo.cdot_vc.activities.voip
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.window.layout.FoldingFeature
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.*
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.voip.viewmodels.CallsViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.ConferenceViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.ControlsViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.StatisticsListViewModel
import com.naminfo.cdot_vc.compatibility.Compatibility
import com.naminfo.cdot_vc.core.CoreService
import org.linphone.core.Call
import org.linphone.core.GlobalState
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.databinding.ActivityCallBinding
import org.linphone.mediastream.Version
import com.naminfo.cdot_vc.utils.PermissionHelper
import com.naminfo.cdot_vc.utils.StatusBarUtils


class CallActivity : ProximitySensorActivity() {

    private lateinit var binding: ActivityCallBinding
    private lateinit var controlsViewModel: ControlsViewModel
    private lateinit var callsViewModel: CallsViewModel
    private lateinit var conferenceViewModel: ConferenceViewModel
    private lateinit var statsViewModel: StatisticsListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Version.sdkStrictlyBelow(Version.API27_OREO_81)) {
            Compatibility.setShowWhenLocked(this, true)
            Compatibility.setTurnScreenOn(this, true)
            Compatibility.requestDismissKeyguard(this)
        }
        super.onCreate(savedInstanceState)
        android.util.Log.d("CDOT_VC", "=================>>>>>>>>>>onCreate: ")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)
        binding.lifecycleOwner = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StatusBarUtils.setStatusBar(this, R.color.systembar_primary_color, lightIcons = false)
            StatusBarUtils.showSystemBars(this)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val navController = binding.navHostFragment.findNavController()
        val storeOwner = navController.getViewModelStoreOwner(R.id.call_nav_graph)

        controlsViewModel = ViewModelProvider(storeOwner)[ControlsViewModel::class.java]
        callsViewModel = ViewModelProvider(storeOwner)[CallsViewModel::class.java]
        conferenceViewModel = ViewModelProvider(storeOwner)[ConferenceViewModel::class.java]
        statsViewModel = ViewModelProvider(storeOwner)[StatisticsListViewModel::class.java]
        binding.controlsViewModel = controlsViewModel

        setupObservers()
        checkPermissions()
    }

    // region ==== Observers & UI ====
    private fun setupObservers() {
        controlsViewModel.askPermissionEvent.observe(this) {
            it.consume { requestPermissions(arrayOf(it), 0) }
        }
        controlsViewModel.fullScreenMode.observe(this) {
            Compatibility.hideAndroidSystemUI(it, window)
        }
        controlsViewModel.proximitySensorEnabled.observe(this) { enableProximitySensor(it) }

        controlsViewModel.isVideoEnabled.observe(this) { enabled ->
            Compatibility.enableAutoEnterPiP(
                this,
                enabled,
                conferenceViewModel.conferenceExists.value == true
            )
        }

        controlsViewModel.callStatsVisible.observe(this) {
            if (it) statsViewModel.enable() else statsViewModel.disable()
        }

        callsViewModel.noMoreCallEvent.observe(this) {
            it.consume { if (it) finish() }
        }

        callsViewModel.currentCallData.observe(this) {
            navigateToActiveCall()
        }

        callsViewModel.askPermissionEvent.observe(this) {
            it.consume { requestPermissions(arrayOf(it), 0) }
        }
    }
    // endregion

    // region ==== Lifecycle ====
    override fun onResume() {
        super.onResume()
        Log.e("CDOT_VC", " Core unavailable: onResume")
        val core = try {
            coreContext.core
        } catch (e: Exception) {
            Log.e("CallActivity", "Core unavailable: ${e.message}")
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (core.globalState != GlobalState.On) return@postDelayed
            try {
                core.isVideoCaptureEnabled = true
                core.isVideoDisplayEnabled = true
                updateVideoPolicy(true)
            } catch (e: Exception) {
                Log.e("CallActivity", "VideoPolicy error: ${e.message}")
            }

            coreContext.removeCallOverlay()
            handleCurrentCallState()
        }, 150)
    }

    override fun onPause() {
        val core = coreContext.core
        if (core.callsNb > 0 && core.currentCall != null) {
            coreContext.createCallOverlay()
        }
        super.onPause()
    }

    override fun onDestroy() {
        val core = coreContext.core
        if (core.globalState != GlobalState.Off) {
            core.nativeVideoWindowId = null
            core.nativePreviewWindowId = null
        }
        super.onDestroy()
    }
    // endregion

    // region ==== Call & Conference Handling ====
    private fun handleCurrentCallState() {
        val core = coreContext.core
        val currentCall = core.currentCall

        if (currentCall == null || currentCall.state == Call.State.End || currentCall.state == Call.State.Released) {
            Log.w("CallActivity", "No valid call found, returning to main")
            returnToMain()
            return
        }

        when (currentCall.state) {
            Call.State.OutgoingInit,
            Call.State.OutgoingProgress,
            Call.State.OutgoingEarlyMedia,
            Call.State.OutgoingRinging -> navigateToOutgoingCall()

            Call.State.IncomingReceived,
            Call.State.IncomingEarlyMedia -> {
                val earlyVideo =
                    corePreferences.acceptEarlyMedia &&
                            currentCall.state == Call.State.IncomingEarlyMedia &&
                            currentCall.currentParams.isVideoEnabled
                navigateToIncomingCall(earlyVideo)
            }

            else -> {
                // Auto-merge into MCU if multiple active calls
                if (core.callsNb >= 1 ) {
                    Log.d("CallActivity", "Merging calls into conference (MCX)")
                    core.addAllToConference()
                }

                if (coreContext.core.searchConference(core.currentCallRemoteAddress!!) != null) {
                   // navigateToConferenceCall()
                } else {
                    navigateToActiveCall()
                }
            }
        }
    }

    private fun returnToMain() {
        if (isTaskRoot) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        finish()
    }
    // endregion

    // region ==== Permissions ====
    private fun checkPermissions() {
        val missing = mutableListOf<String>()
        if (!PermissionHelper.get().hasRecordAudioPermission())
            missing.add(Manifest.permission.RECORD_AUDIO)

        if (callsViewModel.currentCallData.value?.call?.currentParams?.isVideoEnabled == true &&
            !PermissionHelper.get().hasCameraPermission()
        ) {
            missing.add(Manifest.permission.CAMERA)
        }

        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 0)
    }

    override fun onRequestPermissionsResult(
        reqCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (reqCode == 0) {
            for (i in permissions.indices) {
                when (permissions[i]) {
                    Manifest.permission.RECORD_AUDIO ->
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                            callsViewModel.updateMicState()

                    Manifest.permission.CAMERA ->
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            coreContext.core.reloadVideoDevices()
                            controlsViewModel.toggleVideo()
                        }
                }
            }
        }
        super.onRequestPermissionsResult(reqCode, permissions, grantResults)
    }
    // endregion

    // region ==== Video Policy ====
    private fun updateVideoPolicy(enable: Boolean) {
        val core = coreContext.core ?: return
        if (core.globalState != GlobalState.On) return

        runCatching {
            val cloned = core.videoActivationPolicy.clone()
            cloned.automaticallyInitiate = enable
            cloned.automaticallyAccept = enable
            core.videoActivationPolicy = cloned
            Log.i("CallActivity", "VideoActivationPolicy updated → $enable")
        }.onFailure {
            Log.e("CallActivity", "Video policy update failed: ${it.message}")
        }
    }
    // endregion

    // region ==== PiP & Folding ====
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val call = coreContext.core.currentCall
        if (call?.currentParams?.isVideoEnabled == true) {
            Compatibility.enterPipMode(this, conferenceViewModel.conferenceExists.value == true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPiP: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig)
        if (::controlsViewModel.isInitialized) controlsViewModel.pipMode.value = isInPiP
    }

    override fun onLayoutChanges(foldingFeature: FoldingFeature?) {
        foldingFeature ?: return
        controlsViewModel.foldingState.value = foldingFeature
    }
    // endregion
}



/*class CallActivity : ProximitySensorActivity() {
    private lateinit var binding: ActivityCallBinding
    private lateinit var controlsViewModel: ControlsViewModel
    private lateinit var callsViewModel: CallsViewModel
    private lateinit var conferenceViewModel: ConferenceViewModel
    private lateinit var statsViewModel: StatisticsListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Flag in manifest should be enough starting Android 8.1
        if (Version.sdkStrictlyBelow(Version.API27_OREO_81)) {
            Compatibility.setShowWhenLocked(this, true)
            Compatibility.setTurnScreenOn(this, true)
            Compatibility.requestDismissKeyguard(this)
        }

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)
        binding.lifecycleOwner = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StatusBarUtils.setStatusBar(
                this@CallActivity,
                R.color.systembar_primary_color,
                lightIcons = false // white icons
            )
            StatusBarUtils.showSystemBars(this@CallActivity)
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // This can't be done in onCreate(), has to be at least in onPostCreate() !
        val navController = binding.navHostFragment.findNavController()
        val navControllerStoreOwner = navController.getViewModelStoreOwner(R.id.call_nav_graph)

        controlsViewModel = ViewModelProvider(navControllerStoreOwner)[ControlsViewModel::class.java]
        binding.controlsViewModel = controlsViewModel

        callsViewModel = ViewModelProvider(navControllerStoreOwner)[CallsViewModel::class.java]

        conferenceViewModel = ViewModelProvider(navControllerStoreOwner)[ConferenceViewModel::class.java]

        statsViewModel = ViewModelProvider(navControllerStoreOwner)[StatisticsListViewModel::class.java]

        val isInPipMode = Compatibility.isInPictureInPictureMode(this)
        Log.i("[Call Activity] onPostCreate: is in PiP mode? $isInPipMode")
        controlsViewModel.pipMode.value = isInPipMode

        controlsViewModel.askPermissionEvent.observe(
            this
        ) {
            it.consume { permission ->
                Log.i("[Call Activity] Asking for $permission permission")
                requestPermissions(arrayOf(permission), 0)
            }
        }

        controlsViewModel.fullScreenMode.observe(
            this
        ) { hide ->
            Compatibility.hideAndroidSystemUI(hide, window)
        }

        controlsViewModel.proximitySensorEnabled.observe(
            this
        ) { enabled ->
            Log.i(
                "[Call Activity] ${if (enabled) "Enabling" else "Disabling"} proximity sensor (if possible)"
            )
            enableProximitySensor(enabled)
        }

        controlsViewModel.isVideoEnabled.observe(
            this
        ) { enabled ->
            Compatibility.enableAutoEnterPiP(
                this,
                enabled,
                conferenceViewModel.conferenceExists.value == true
            )
        }

        controlsViewModel.callStatsVisible.observe(
            this
        ) { visible ->
            if (visible) statsViewModel.enable() else statsViewModel.disable()
        }

        callsViewModel.noMoreCallEvent.observe(
            this
        ) {
            it.consume { noMoreCall ->
                if (noMoreCall) {
                    Log.i("[Call Activity] No more call event fired, finishing activity")
                    finish()
                }
            }
        }

        callsViewModel.currentCallData.observe(
            this
        ) { callData ->
            val call = callData.call
            navigateToActiveCall()
        }

        callsViewModel.askPermissionEvent.observe(
            this
        ) {
            it.consume { permission ->
                Log.i("[Call Activity] Asking for $permission permission")
                requestPermissions(arrayOf(permission), 0)
            }
        }

        *//*conferenceViewModel.conferenceExists.observe(
            this
        ) { exists ->
            if (exists) {
                Log.i(
                    "[Call Activity] Found active conference, changing  switching to ConferenceCall fragment"
                )
                navigateToConferenceCall()
            } else if (coreContext.core.callsNb > 0) {
                Log.i(
                    "[Call Activity] Conference no longer exists, switching to SingleCall fragment"
                )
                navigateToActiveCall()
            }
        }

        conferenceViewModel.isConferenceLocallyPaused.observe(
            this
        ) { paused ->
            if (!paused) {
                Log.i("[Call Activity] Entered conference, make sure conference fragment is active")
                navigateToConferenceCall()
            }
        }*//*

        checkPermissions()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (coreContext.core.currentCall?.currentParams?.isVideoEnabled == true) {
            Log.i("[Call Activity] Entering PiP mode")
            Compatibility.enterPipMode(this, conferenceViewModel.conferenceExists.value == true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        Log.i(
            "[Call Activity] onPictureInPictureModeChanged: is in PiP mode? $isInPictureInPictureMode"
        )
        if (::controlsViewModel.isInitialized) {
            // To hide UI except for TextureViews
            controlsViewModel.pipMode.value = isInPictureInPictureMode
        }
    }

    override fun onResume() {
        super.onResume()
       try {
          // coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
         //  coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
           coreContext.core.isVideoCaptureEnabled = true // Enable video capture
           coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled

           updateVideoActivationPolicy(true)
       }catch (e: Exception){}

           coreContext.removeCallOverlay()

        val currentCall = coreContext.core.currentCall
        Log.i("Incoming - Accept early media - ${corePreferences.acceptEarlyMedia}")

        when (currentCall?.state) {
            Call.State.OutgoingInit, Call.State.OutgoingEarlyMedia, Call.State.OutgoingProgress, Call.State.OutgoingRinging -> {
                navigateToOutgoingCall()
            }
            Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                Log.i("Incoming - Is video enabled - ${currentCall.params.isVideoEnabled}")
                val earlyMediaVideoEnabled = corePreferences.acceptEarlyMedia &&
                        currentCall.state == Call.State.IncomingEarlyMedia &&
                        currentCall.currentParams.isVideoEnabled
                navigateToIncomingCall(earlyMediaVideoEnabled)
            }
            else -> {}
        }
        if (coreContext.core.callsNb == 0) {
            Log.w("[Call Activity] Resuming but no call found...")
            if (isTaskRoot) {
                // When resuming app from recent tasks make sure MainActivity will be launched if there is no call
                val intent = Intent()
                intent.setClass(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                finish()
            }
            return
        }
    }

    override fun onPause() {
        val core = coreContext.core
        if (core.callsNb > 0) {
            coreContext.createCallOverlay()
        }

        super.onPause()
    }

    override fun onDestroy() {
        if (coreContext.core.globalState != GlobalState.Off) {
            coreContext.core.nativeVideoWindowId = null
            coreContext.core.nativePreviewWindowId = null
        }

        super.onDestroy()
    }

    private fun checkPermissions() {
        val permissionsRequiredList = arrayListOf<String>()

        if (!PermissionHelper.get().hasRecordAudioPermission()) {
            Log.i("[Call Activity] Asking for RECORD_AUDIO permission")
            permissionsRequiredList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (callsViewModel.currentCallData.value?.call?.currentParams?.isVideoEnabled == true &&
            !PermissionHelper.get().hasCameraPermission()
        ) {
            Log.i("[Call Activity] Asking for CAMERA permission")
            permissionsRequiredList.add(Manifest.permission.CAMERA)
        }

        if (permissionsRequiredList.isNotEmpty()) {
            val permissionsRequired = arrayOfNulls<String>(permissionsRequiredList.size)
            permissionsRequiredList.toArray(permissionsRequired)
            requestPermissions(permissionsRequired, 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            for (i in permissions.indices) {
                when (permissions[i]) {
                    Manifest.permission.RECORD_AUDIO -> if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.i("[Call Activity] RECORD_AUDIO permission has been granted")
                        callsViewModel.updateMicState()
                    }
                    Manifest.permission.CAMERA -> if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.i("[Call Activity] CAMERA permission has been granted")
                        coreContext.core.reloadVideoDevices()
                        controlsViewModel.toggleVideo()
                    }
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.i(
                            "[Call Activity] WRITE_EXTERNAL_STORAGE permission has been granted, taking snapshot"
                        )
                        controlsViewModel.takeSnapshot()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onLayoutChanges(foldingFeature: FoldingFeature?) {
        foldingFeature ?: return
        Log.i(
            "[Call Activity] Folding feature state changed: ${foldingFeature.state}, orientation is ${foldingFeature.orientation}"
        )

        controlsViewModel.foldingState.value = foldingFeature
    }

   *//* fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }*//*
    private fun updateVideoActivationPolicy(enable: Boolean) {
        val core = LinphoneApplication.coreContext.core ?: return
        val policy = core.videoActivationPolicy
        val newPolicy = policy.clone()  // ✅ make a modifiable copy

        newPolicy.automaticallyInitiate = true
        newPolicy.automaticallyAccept = true
        core.videoActivationPolicy = newPolicy  // ✅ apply it back
    }

}*/
