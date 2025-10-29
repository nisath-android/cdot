package com.naminfo.cdot_vc.activities.voip.viewmodels

import android.Manifest
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.window.layout.FoldingFeature
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.MediaDirection
import com.naminfo.cdot_vc.utils.*
import org.linphone.core.tools.Log
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class ControlsViewModel  : ViewModel() {
    val isSpeakerSelected = MutableLiveData<Boolean>()

    val isBluetoothHeadsetSelected = MutableLiveData<Boolean>()

    val audioRoutesSelected = MutableLiveData<Boolean>()

    val audioRoutesEnabled = MutableLiveData<Boolean>()

    val isVideoAvailable = MutableLiveData<Boolean>()

    val isVideoEnabled = MutableLiveData<Boolean>()

    val isSendingVideo = MutableLiveData<Boolean>()

    val isVideoUpdateInProgress = MutableLiveData<Boolean>()

    val isSwitchCameraAvailable = MutableLiveData<Boolean>()

    val isOutgoingEarlyMedia = MutableLiveData<Boolean>()

    val isIncomingEarlyMediaVideo = MutableLiveData<Boolean>()

    val isIncomingCallVideo = MutableLiveData<Boolean>()

    val showExtras = MutableLiveData<Boolean>()

    val fullScreenMode = MutableLiveData<Boolean>()

    val folded = MutableLiveData<Boolean>()

    val pipMode = MutableLiveData<Boolean>()

    val chatRoomCreationInProgress = MutableLiveData<Boolean>()

    val numpadVisible = MutableLiveData<Boolean>()

    val dtmfHistory = MutableLiveData<String>()

    val callStatsVisible = MutableLiveData<Boolean>()

    val proximitySensorEnabled = MediatorLiveData<Boolean>()

    val forceDisableProximitySensor = MutableLiveData<Boolean>()

    val showTakeSnapshotButton = MutableLiveData<Boolean>()

    val attendedTransfer = MutableLiveData<Boolean>()

    val chatDisabled = MutableLiveData<Boolean>()

    val goToConferenceParticipantsListEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val goToChatEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val goToCallsListEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val goToConferenceLayoutSettingsEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val askPermissionEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    val goToDialerEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val foldingState = MutableLiveData<FoldingFeature>()

    val hideVideo = corePreferences.disableVideo

    private val nonEarpieceOutputAudioDevice = MutableLiveData<Boolean>()
    val cdrDetails: ArrayList<Array<String>> = ArrayList()

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            Log.i("[Call Controls] State changed: $state")
            Log.i(
                "[Call Controls] Current call reason terminate: ${call.reason}"
            )

            isOutgoingEarlyMedia.value = state == Call.State.OutgoingEarlyMedia
            isIncomingEarlyMediaVideo.value = state == Call.State.IncomingEarlyMedia && call.remoteParams?.isVideoEnabled == true
            isIncomingCallVideo.value = call.remoteParams?.isVideoEnabled == true && coreContext.core.videoActivationPolicy.automaticallyAccept
            attendedTransfer.value = core.callsNb > 1

            // if (state == Call.State.Connected) fetchPbxContacts()
            if (state == Call.State.StreamsRunning) {
                Log.i(
                    "[Call Controls] Current call video enabled: ${core.currentCall?.currentParams?.isVideoEnabled}" +
                            "Remote call video enabled: ${call.remoteParams?.isVideoEnabled}\"" +
                            "current params - ${call.currentParams.isVideoEnabled}"
                )
                if (!call.currentParams.isVideoEnabled && fullScreenMode.value == true) {
                    fullScreenMode.value = false
                }
                isVideoUpdateInProgress.value = false
                proximitySensorEnabled.value = shouldProximitySensorBeEnabled()
            } else if (state == Call.State.PausedByRemote) {
                fullScreenMode.value = false
            }

            if (core.currentCall?.currentParams?.isVideoEnabled == true && !PermissionHelper.get().hasCameraPermission()) {
                askPermissionEvent.value = Event(Manifest.permission.CAMERA)
            }
            if (state == Call.State.End) {
                Log.i("[Call Controls] Domain : ${core.authInfoList[0].domain}")
                Log.i("[Call Controls] Call end state: $state, ${call.reason}")
                Log.i(
                    "[Call Controls] Current state is before terminate: ${call.state}"
                )
                Log.i("[Call Controls] Current call direction: ${call.dir}")
                Log.i("[Call Controls] Current call reason terminate: ${call.reason}")
                Log.i("[Call Controls] Current details duration ${call.duration}")
                Log.i(
                    "[Call Controls] Current details remote address ${call.remoteAddress.username}"
                )
                Log.i(
                    "[Call Controls] Current details from address ${call.callLog.fromAddress.username}"
                )
                Log.i(
                    "[Call Controls] Current details start time ${call.callLog.startDate}"
                )
                Log.i("[Call Controls] Current details status ${call.callLog.status}")

                val domain = core.authInfoList[0].domain
                var duration = call.duration
                val endTime = getCurrentFormattedDate()
                var remoteAddress: String = call.remoteAddress.username.toString()
                val fromAddress: String = call.callLog.fromAddress.username.toString()
                val address: String = call.callLog.localAddress.username.toString()
                val startTimeLong: String = call.callLog.startDate.toString()
                val uuid = fromAddress + startTimeLong
                val startTime = convertUnixTimestampToFormattedDate(startTimeLong.toLong())
                var reason = call.reason.toString()
                if (reason == "None") {
                    reason = "Normal Disconnect"
                } else {
                    reason = "NR/Busy"
                    duration = 0
                }
                val direction1 = call.dir
                var direction = direction1.toString()
                if (direction.equals("Incoming")) remoteAddress = address
                var callTypes = ""
                if (call.isCameraEnabled) {
                    callTypes = "2"
                    if (remoteAddress.equals("7000")) {
                        callTypes = "6"
                        Log.i(
                            "[Call Controls] Current details call type $callTypes"
                        )
                    }
                } else {
                    callTypes = "1"
                    if (remoteAddress.equals("3500")) {
                        callTypes = "5"
                        Log.i(
                            "[Call Controls] Current details call type $callTypes"
                        )
                    }
                }

                Log.i("[Call Controls] Current details call type $callTypes")
                Log.i("[Call Controls] Current details start time $startTime end time $endTime")

                cdrDetails.add(arrayOf("Subscriber_ID", fromAddress))
                cdrDetails.add(arrayOf("Call_Ref_ID", uuid))
                cdrDetails.add(arrayOf("Calling_Number", fromAddress))
                cdrDetails.add(arrayOf("Called_Number", remoteAddress))
                cdrDetails.add(arrayOf("Call_Type", direction))
                cdrDetails.add(arrayOf("Call_Offer_Time", startTime))
                cdrDetails.add(arrayOf("Call_Connected_Time", startTime))
                cdrDetails.add(arrayOf("Call_disConnected_Time", endTime))
                cdrDetails.add(arrayOf("Call_Duration", duration.toString()))
                cdrDetails.add(arrayOf("Disconnected_Reason", reason))
                cdrDetails.add(arrayOf("calltype", callTypes))
                cdrDetails.add(arrayOf("Calling_No_Ip", ""))
                cdrDetails.add(arrayOf("Called_No_Ip", ""))
                cdrDetails.add(arrayOf("IMEI_NO", ""))
                cdrDetails.add(arrayOf("IMSI_NO", ""))
                cdrDetails.add(arrayOf("Network_Mode", ""))
                cdrDetails.add(arrayOf("Calling_Number_City", ""))
                cdrDetails.add(arrayOf("Called_Number_City", ""))
                cdrDetails.add(arrayOf("Calling_Number_Country", ""))
                cdrDetails.add(arrayOf("Called_Number_Country", ""))
                cdrDetails.add(arrayOf("CAS_EXPORT_F", ""))
                cdrDetails.add(arrayOf("BWinMB", ""))
                cdrDetails.add(arrayOf("filename", ""))
                cdrDetails.add(arrayOf("filesize", ""))
                cdrDetails.add(arrayOf("IM_Message_TIME", startTime))
                cdrDetails.add(arrayOf("Reason", reason))
                cdrDetails.add(arrayOf("groupid", ""))
                cdrDetails.add(arrayOf("missedcallstatus", ""))
                cdrDetails.add(arrayOf("callednumbernetworkmode", ""))
                cdrDetails.add(arrayOf("conferenceid", ""))
                cdrDetails.add(arrayOf("message", ""))
                cdrDetails.add(arrayOf("Encryption", ""))
                cdrDetails.add(arrayOf("Groupname", ""))

                val detailsStringBuilder = StringBuilder("Call ended, CDR details:")
                for (detail in cdrDetails) {
                    detailsStringBuilder.append(" ${detail.contentToString()}")
                }

                Log.i("[Call Controls]", detailsStringBuilder.toString())
                sendCdrUsingGetRequest(cdrDetails, domain)
            }

            updateUI()
        }

        override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
            Log.i("[Call Controls] Audio device changed: ${audioDevice.deviceName}")

            nonEarpieceOutputAudioDevice.value = audioDevice.type != AudioDevice.Type.Earpiece
            updateSpeakerState()
            updateBluetoothHeadsetState()
        }

        override fun onAudioDevicesListUpdated(core: Core) {
            Log.i("[Call Controls] Audio devices list updated")
            val wasBluetoothPreviouslyAvailable = audioRoutesEnabled.value == true
            updateAudioRoutesState()
            /*if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
                AudioRouteUtils.routeAudioToHeadset()
            } else if (!wasBluetoothPreviouslyAvailable && corePreferences.routeAudioToBluetoothIfAvailable) {
                // Only attempt to route audio to bluetooth automatically when bluetooth device is connected
                if (AudioRouteUtils.isBluetoothAudioRouteAvailable()) {
                    AudioRouteUtils.routeAudioToBluetooth()
                }
            }*/
        }
    }

    val extraButtonsMenuTranslateY = MutableLiveData<Float>()
    private val extraButtonsMenuAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(
            AppUtils.getDimension(R.dimen.voip_call_extra_buttons_translate_y),
            0f
        ).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                extraButtonsMenuTranslateY.value = value
            }
            duration = if (corePreferences.enableAnimations) 500 else 0
        }
    }

    val audioRoutesMenuTranslateY = MutableLiveData<Float>()
    private val audioRoutesMenuAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(AppUtils.getDimension(R.dimen.voip_audio_routes_menu_translate_y), 0f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                audioRoutesMenuTranslateY.value = value
            }
            duration = if (corePreferences.enableAnimations) 500 else 0
        }
    }

    val bouncyCounterTranslateY = MutableLiveData<Float>()

    private val bounceAnimator: ValueAnimator by lazy {
        ValueAnimator.ofFloat(AppUtils.getDimension(R.dimen.voip_counter_bounce_offset), 0f).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                bouncyCounterTranslateY.value = value
            }
            interpolator = LinearInterpolator()
            duration = 250
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
    }

    init {
        coreContext.core.addListener(listener)

        chatDisabled.value = corePreferences.disableChat
        fullScreenMode.value = false
        extraButtonsMenuTranslateY.value = AppUtils.getDimension(
            R.dimen.voip_call_extra_buttons_translate_y
        )
        audioRoutesMenuTranslateY.value = AppUtils.getDimension(
            R.dimen.voip_audio_routes_menu_translate_y
        )
        audioRoutesSelected.value = false
        forceDisableProximitySensor.value = false

        nonEarpieceOutputAudioDevice.value = coreContext.core.outputAudioDevice?.type != AudioDevice.Type.Earpiece
        proximitySensorEnabled.value = shouldProximitySensorBeEnabled()
        proximitySensorEnabled.addSource(isVideoEnabled) {
            proximitySensorEnabled.value = shouldProximitySensorBeEnabled()
        }
        proximitySensorEnabled.addSource(nonEarpieceOutputAudioDevice) {
            proximitySensorEnabled.value = shouldProximitySensorBeEnabled()
        }
        proximitySensorEnabled.addSource(forceDisableProximitySensor) {
            proximitySensorEnabled.value = shouldProximitySensorBeEnabled()
        }

        val currentCall = coreContext.core.currentCall ?: coreContext.core.calls.firstOrNull()
        val state = currentCall?.state ?: Call.State.Idle
        Log.i("[Call Controls] Current state is: $state")
        isOutgoingEarlyMedia.value = state == Call.State.OutgoingEarlyMedia
        isIncomingEarlyMediaVideo.value = state == Call.State.IncomingEarlyMedia && currentCall?.remoteParams?.isVideoEnabled == true
        isIncomingCallVideo.value = currentCall?.remoteParams?.isVideoEnabled == true && coreContext.core.videoActivationPolicy.automaticallyAccept

        updateUI()

        if (corePreferences.enableAnimations) bounceAnimator.start()
    }

    override fun onCleared() {
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    fun hangUp() {
        val core = coreContext.core
        // core.createCallLog()
        when {
            core.currentCall != null -> {
                core.currentCall?.terminate()
                // Log.i("[Call Controls] Final Current state is: ${core.currentCall!!.state}")
            }
            else -> {
                core.terminateAllCalls()
            }
        }
    }

    fun answer() {
        val currentCall = coreContext.core.currentCall ?: coreContext.core.calls.find {
                call ->
            call.state == Call.State.IncomingReceived || call.state == Call.State.IncomingEarlyMedia
        }
        if (currentCall != null) {
            coreContext.answerCall(currentCall)
        } else {
            Log.e("[Controls] Can't find any current call to answer")
        }
    }

    fun toggleSpeaker() {
        if (AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()) {
            forceEarpieceAudioRoute()
        } else {
            forceSpeakerAudioRoute()
        }
    }

    fun toggleRoutesMenu() {
        audioRoutesSelected.value = audioRoutesSelected.value != true
        if (audioRoutesSelected.value == true) {
            audioRoutesMenuAnimator.start()
        } else {
            audioRoutesMenuAnimator.reverse()
        }
    }

    fun forceEarpieceAudioRoute() {
        if (AudioRouteUtils.isHeadsetAudioRouteAvailable()) {
            Log.i("[Call Controls] Headset found, route audio to it instead of earpiece")
            AudioRouteUtils.routeAudioToHeadset()
        } else {
            AudioRouteUtils.routeAudioToEarpiece()
        }
        updateSpeakerState()
        updateBluetoothHeadsetState()
    }

    fun forceSpeakerAudioRoute() {
        AudioRouteUtils.routeAudioToSpeaker()
        updateSpeakerState()
        updateBluetoothHeadsetState()
    }

    fun forceBluetoothAudioRoute() {
        AudioRouteUtils.routeAudioToBluetooth()
        updateSpeakerState()
        updateBluetoothHeadsetState()
    }

    fun toggleVideo() {
        if (!PermissionHelper.get().hasCameraPermission()) {
            Log.w(
                "[Call Controls] Camera permission isn't granted, asking it before toggling video"
            )
            askPermissionEvent.value = Event(Manifest.permission.CAMERA)
            return
        }

        val core = coreContext.core
        val currentCall = core.currentCall
        if (currentCall != null) {
            val state = currentCall.state
            if (state == Call.State.End || state == Call.State.Released || state == Call.State.Error) {
                Log.e("[Call Controls] Current call state is $state, aborting video toggle")
                return
            }

            isVideoUpdateInProgress.value = true
            val params = core.createCallParams(currentCall)
            if (currentCall.conference != null) {
                if (params?.isVideoEnabled == false) {
                    params.isVideoEnabled = true
                    params.videoDirection = MediaDirection.SendRecv
                } else {
                    if (params?.videoDirection == MediaDirection.SendRecv || params?.videoDirection == MediaDirection.SendOnly) {
                        params.videoDirection = MediaDirection.RecvOnly
                    } else {
                        params?.videoDirection = MediaDirection.SendRecv
                    }
                }
            } else {
                params?.isVideoEnabled = params?.isVideoEnabled == false
                Log.i(
                    "[Call Controls] Updating call with video enabled set to ${params?.isVideoEnabled}"
                )
            }
            currentCall.update(params)
        } else {
            Log.e("[Call Controls] Can't toggle video, no current call found!")
        }
    }

    fun switchCamera() {
        coreContext.switchCamera()
    }

    fun takeSnapshot() {
        if (!PermissionHelper.get().hasWriteExternalStoragePermission()) {
            askPermissionEvent.value = Event(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            val currentCall = coreContext.core.currentCall
            if (currentCall != null && currentCall.currentParams.isVideoEnabled) {
                val fileName = System.currentTimeMillis().toString() + ".jpeg"
                val fullPath = FileUtils.getFileStoragePath(fileName).absolutePath
                Log.i("[Call Controls] Snapshot will be save under $fullPath")
                currentCall.takeVideoSnapshot(fullPath)
            } else {
                Log.e("[Call Controls] Current call doesn't have video, can't take snapshot")
            }
        }
    }

    fun showExtraButtons() {
        extraButtonsMenuAnimator.start()
        showExtras.value = true
    }

    fun hideExtraButtons(skipAnimation: Boolean) {
        // Animation must be skipped when called from Fragment's onPause() !
        if (skipAnimation) {
            extraButtonsMenuTranslateY.value = AppUtils.getDimension(
                R.dimen.voip_call_extra_buttons_translate_y
            )
        } else {
            extraButtonsMenuAnimator.reverse()
        }
        showExtras.value = false
        chatRoomCreationInProgress.value = false
    }

    fun toggleFullScreen() {
        if (fullScreenMode.value == false && isVideoEnabled.value == false) return
        fullScreenMode.value = fullScreenMode.value != true
    }

    fun goToConferenceParticipantsList() {
        goToConferenceParticipantsListEvent.value = Event(true)
    }

    fun goToChat() {
        chatRoomCreationInProgress.value = true
        goToChatEvent.value = Event(true)
    }

    fun showNumpad() {
        hideExtraButtons(false)
        numpadVisible.value = true
    }

    fun hideNumpad() {
        numpadVisible.value = false
    }

    fun handleDtmfClick(key: Char) {
        dtmfHistory.value = "${dtmfHistory.value.orEmpty()}$key"
        coreContext.core.playDtmf(key, 1)
        coreContext.core.currentCall?.sendDtmf(key)
    }

    fun goToCallsList() {
        goToCallsListEvent.value = Event(true)
    }

    fun showCallStats(skipAnimation: Boolean = false) {
        hideExtraButtons(skipAnimation)
        callStatsVisible.value = true
    }

    fun hideCallStats() {
        callStatsVisible.value = false
    }

    fun goToConferenceLayout() {
        goToConferenceLayoutSettingsEvent.value = Event(true)
    }

    fun transferCall() {
        // In case there is more than 1 call, transfer will be attended instead of blind
        if (coreContext.core.callsNb > 1) {
            attendedTransfer()
        } else {
            goToDialerForCallTransfer()
        }
    }

    private fun attendedTransfer() {
        val core = coreContext.core
        val currentCall = core.currentCall

        if (currentCall == null) {
            Log.e("[Call Controls] Can't do an attended transfer without a current call")
            return
        }
        if (core.callsNb <= 1) {
            Log.e("[Call Controls] Need at least two calls to do an attended transfer")
            return
        }

        val callToTransferTo = core.calls.findLast {
            it.state == Call.State.Paused
        }
        if (callToTransferTo == null) {
            Log.e(
                "[Call Controls] Couldn't find a call in Paused state to transfer current call to"
            )
            return
        }

        Log.i(
            "[Call Controls] Doing an attended transfer between active call [${currentCall.remoteAddress.asStringUriOnly()}] and paused call [${callToTransferTo.remoteAddress.asStringUriOnly()}]"
        )
        val result = callToTransferTo.transferToAnother(currentCall)
        if (result != 0) {
            Log.e("[Call Controls] Attended transfer failed!")
        }
    }

    private fun goToDialerForCallTransfer() {
        goToDialerEvent.value = Event(true)
    }

    fun goToDialerForNewCall() {
        goToDialerEvent.value = Event(false)
    }

    private fun updateUI() {
        updateVideoAvailable()
        updateVideoEnabled()
        updateSpeakerState()
        updateBluetoothHeadsetState()
        updateAudioRoutesState()
    }

    private fun updateSpeakerState() {
        isSpeakerSelected.value = AudioRouteUtils.isSpeakerAudioRouteCurrentlyUsed()
    }

    private fun updateBluetoothHeadsetState() {
        isBluetoothHeadsetSelected.value = AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()
    }

    private fun updateAudioRoutesState() {
        val bluetoothDeviceAvailable = AudioRouteUtils.isBluetoothAudioRouteAvailable()
        audioRoutesEnabled.value = bluetoothDeviceAvailable

        if (!bluetoothDeviceAvailable) {
            audioRoutesSelected.value = false
        }
    }

    private fun updateVideoAvailable() {
        val core = coreContext.core
        val currentCall = core.currentCall
        isVideoAvailable.value = (core.isVideoCaptureEnabled || core.isVideoPreviewEnabled) &&
                (currentCall != null && !currentCall.mediaInProgress())
    }

    private fun updateVideoEnabled() {
        val currentCall = coreContext.core.currentCall ?: coreContext.core.calls.firstOrNull()
        val enabled = currentCall?.currentParams?.isVideoEnabled ?: false
        // Prevent speaker to turn on each time a participant joins a video conference
        val isConference = currentCall?.conference != null
        if (enabled && !isConference && isVideoEnabled.value == false) {
            Log.i("[Call Controls] Video is being turned on")
            if (corePreferences.routeAudioToSpeakerWhenVideoIsEnabled) {
                // Do not turn speaker on when video is enabled if headset or bluetooth is used
                if (!AudioRouteUtils.isHeadsetAudioRouteAvailable() &&
                    !AudioRouteUtils.isBluetoothAudioRouteCurrentlyUsed()
                ) {
                    Log.i(
                        "[Call Controls] Video enabled and no wired headset not bluetooth in use, routing audio to speaker"
                    )
                    AudioRouteUtils.routeAudioToSpeaker()
                }
            }
        }

        isVideoEnabled.value = enabled
        showTakeSnapshotButton.value = enabled && corePreferences.showScreenshotButton
        val videoDirection = if (coreContext.core.currentCall?.conference != null) {
            coreContext.core.currentCall?.currentParams?.videoDirection
        } else {
            coreContext.core.currentCall?.params?.videoDirection
        }
        val isVideoBeingSent = videoDirection == MediaDirection.SendRecv || videoDirection == MediaDirection.SendOnly
        isSendingVideo.value = isVideoBeingSent
        isSwitchCameraAvailable.value = enabled && coreContext.showSwitchCameraButton() && isVideoBeingSent
    }

    private fun shouldProximitySensorBeEnabled(): Boolean {
        val currentCall = coreContext.core.currentCall ?: coreContext.core.calls.firstOrNull()
        if (currentCall != null) {
            when (val state = currentCall.state) {
                Call.State.OutgoingEarlyMedia, Call.State.OutgoingProgress, Call.State.OutgoingRinging, Call.State.OutgoingInit -> {
                    Log.i(
                        "[Call Controls] Call is in outgoing state [$state], enabling proximity sensor"
                    )
                    return true
                }
                Call.State.IncomingEarlyMedia, Call.State.IncomingReceived -> {
                    Log.i(
                        "[Call Controls] Call is in incoming state [$state], enabling proximity sensor"
                    )
                    return true
                }
                else -> { }
            }
        }

        if (forceDisableProximitySensor.value == true) {
            Log.i(
                "[Call Controls] Forcing proximity sensor to be disabled (usually in incoming/outgoing call fragments)"
            )
        } else if (isVideoEnabled.value == true) {
            Log.i(
                "[Call Controls] Active call current params says video is enabled, proximity sensor will be disabled"
            )
        } else if (nonEarpieceOutputAudioDevice.value == true) {
            Log.i(
                "[Call Controls] Current audio route is not earpiece, proximity sensor will be disabled"
            )
        }

        return forceDisableProximitySensor.value == false &&
                !(isVideoEnabled.value ?: false) &&
                !(nonEarpieceOutputAudioDevice.value ?: false)
    }

    private fun sendCdrUsingGetRequest(cdrDetails: ArrayList<Array<String>>, domain: String?) {
        // Base URL of your web service
        val baseUrl = "http://$domain/fs_webservice/WebService.asmx/Insert_Call_Details_FS"

        // Build query string from cdrDetails
        val queryString = StringBuilder()
        for (param in cdrDetails) {
            if (queryString.isNotEmpty()) {
                queryString.append('&')
            }
            queryString.append(URLEncoder.encode(param[0], "UTF-8"))
            queryString.append('=')
            queryString.append(URLEncoder.encode(param[1], "UTF-8"))
        }

        // Full URL with query parameters
        val fullUrl = "$baseUrl?$queryString"

        // Create a request using OkHttp
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(fullUrl)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("CDR Upload Failure", "Failed to send CDR: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("CDR Upload", "CDR sent successfully: ${response.body?.string()}")
                } else {
                    Log.e(
                        "CDR Upload Error",
                        "Failed to send CDR: ${response.code} - ${response.message}"
                    )
                }
            }
        })
    }

    fun convertUnixTimestampToFormattedDate(unixTimestamp: Long): String {
        // Create a Date object from the Unix timestamp (in milliseconds)
        val date = Date(unixTimestamp * 1000)

        // Define the desired date format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        // Set the timezone to IST
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))

        // Format the Date object to the desired string format
        return dateFormat.format(date)
    }

    fun getCurrentFormattedDate(): String {
        // Get the current time in milliseconds
        val currentTimeMillis = System.currentTimeMillis()

        // Create a Date object from the current time in milliseconds
        val date = Date(currentTimeMillis)

        // Define the desired date format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        // Set the timezone to IST
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))

        // Format the Date object to the desired string format
        return dateFormat.format(date)
    }
}
