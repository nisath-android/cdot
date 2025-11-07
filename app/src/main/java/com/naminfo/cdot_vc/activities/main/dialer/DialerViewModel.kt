package com.naminfo.cdot_vc.activities.main.dialer

import android.content.Context
import android.os.Vibrator
import android.text.Editable
import android.util.Log
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.viewmodels.LogsUploadViewModel
import com.naminfo.cdot_vc.compatibility.Compatibility
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.LinphoneUtils
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.RegistrationState

class DialerViewModel  : LogsUploadViewModel() {
    val enteredUri = MutableLiveData<String>()
    val isVideoClickable = MutableLiveData<Boolean>(true)
    val isAudioClickable = MutableLiveData<Boolean>(true)

    val atLeastOneCall = MutableLiveData<Boolean>()

    val transferVisibility = MutableLiveData<Boolean>()

    val showPreview = MutableLiveData<Boolean>()

    val showSwitchCamera = MutableLiveData<Boolean>()

    val autoInitiateVideoCalls = MutableLiveData<Boolean>()

    val scheduleConferenceAvailable = MutableLiveData<Boolean>()

    val hideAddContactButton = MutableLiveData<Boolean>()

    val updateAvailableEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    val onBeforeUriChanged: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    val onAfterUriChanged: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    val dialogCall: MutableLiveData<Event<Map<String, Boolean>>> by lazy { MutableLiveData<Event<Map<String, Boolean>>>() }
    val takePTT: MutableLiveData<Event<Map<String, Boolean>>> by lazy { MutableLiveData<Event<Map<String, Boolean>>>() }

    private val vibrator =
        coreContext.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var addressWaitingNetworkToBeCalled: String? = null
    private var timeAtWitchWeTriedToCall: Long = 0

    private var enteredUriCursorPosition: Int = 0

    val onKeyClick: NumpadDigitListener = object : NumpadDigitListener {
        override fun handleClick(key: Char) {
            val sb: StringBuilder = StringBuilder(enteredUri.value)
            try {
                Log.i("Dialer","[xxxyyyDialer] enteredUriCursorPosition: $enteredUriCursorPosition || ${key.toString()}")
                sb.insert(enteredUriCursorPosition, key.toString())
            } catch (ioobe: IndexOutOfBoundsException) {
                sb.insert(sb.length, key.toString())
            }
            Log.i("Dialer","[xxxyyyDialer] sb: $sb")
            enteredUri.value = sb.toString()

            if (vibrator.hasVibrator() && corePreferences.dtmfKeypadVibration) {
                Compatibility.eventVibration(vibrator)
            }
        }

        override fun handleLongClick(key: Char): Boolean {
            if (key == '1') {
                val voiceMailUri = corePreferences.voiceMailUri
                if (voiceMailUri != null) {
                    coreContext.startCall(voiceMailUri)
                }
            } else {
                enteredUri.value += key.toString()
            }
            return true
        }
    }

    private val listener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            val isCall = core.callsNb > 0
            atLeastOneCall.value = isCall
            isAudioClickable.value = !isCall
            isVideoClickable.value = !isCall
        }

        override fun onTransferStateChanged(core: Core, transfered: Call, callState: Call.State) {
            if (callState == Call.State.OutgoingProgress) {
                // Will work for both blind & attended transfer
                //onMessageToNotifyEvent.value = Event(R.string.dialer_transfer_succeded)
            }
        }

        override fun onNetworkReachable(core: Core, reachable: Boolean) {
            val address = addressWaitingNetworkToBeCalled.orEmpty()
            if (reachable && address.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (now - timeAtWitchWeTriedToCall > 1000) {
                    Log.e("Dialer",
                        "[Dialer] More than 1 second has passed waiting for network, abort auto call to $address"
                    )
                    enteredUri.value = address
                } else {
                    Log.i("Dialer","[Dialer] Network is available, continue auto call to $address")
                    coreContext.startCall(address)
                }

                addressWaitingNetworkToBeCalled = null
                timeAtWitchWeTriedToCall = 0
            }
        }


        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            scheduleConferenceAvailable.value = LinphoneUtils.isRemoteConferencingAvailable()
        }
    }

    init {
        coreContext.core.addListener(listener)
        val isCall = coreContext.core.callsNb > 0
        enteredUri.value = ""
        atLeastOneCall.value = isCall
        isAudioClickable.value = !isCall
        isVideoClickable.value = !isCall
        transferVisibility.value = false
        hideAddContactButton.value = corePreferences.readOnlyNativeContacts

        showSwitchCamera.value = coreContext.showSwitchCameraButton()
        scheduleConferenceAvailable.value = LinphoneUtils.isRemoteConferencingAvailable()
    }

    override fun onCleared() {
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    // This is to workaround the cursor being set to the start when pressing a digit
    fun onBeforeUriChanged(editText: EditText, count: Int, after: Int) {
        onBeforeUriChanged.value = Event(enteredUri.value ?: "")
        enteredUriCursorPosition = editText.selectionEnd
        enteredUriCursorPosition += after - count
    }

    fun onAfterUriChanged(editText: EditText, editable: Editable?) {
        onAfterUriChanged.value = Event(enteredUri.value ?: "")
        val newLength = editable?.length ?: 0
        if (newLength <= enteredUriCursorPosition) enteredUriCursorPosition = newLength
        if (enteredUriCursorPosition < 0) enteredUriCursorPosition = 0
        editText.setSelection(enteredUriCursorPosition)
    }

    fun updateShowVideoPreview() {
        val videoPreview = corePreferences.videoPreview
        showPreview.value = videoPreview
        coreContext.core.isVideoPreviewEnabled = videoPreview
    }

    fun eraseLastChar() {
        enteredUri.value = enteredUri.value?.dropLast(1)
    }

    fun eraseAll(): Boolean {
        enteredUri.value = ""
        return true
    }

    fun directCall(to: String) {
        if (coreContext.core.isNetworkReachable) {
            coreContext.startCall(to)
        } else {
            Log.w( "Dialer",
                "[xxxDialer] Network isnt't reachable at the time, wait for network to start call (happens mainly when app is cold started)"
            )
            timeAtWitchWeTriedToCall = System.currentTimeMillis()
            addressWaitingNetworkToBeCalled = to
        }
    }

    private fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }

    fun startCall() {
        val addressToCall = enteredUri.value.orEmpty()

        if (addressToCall.isNotEmpty()) {
            // Ensure only audio is enabled before making the call
            Log.i("Dialer","[Dialer] video enable: ${coreContext.core.isVideoEnabled}")
           // coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Disable video initiation
         //   coreContext.core.videoActivationPolicy.automaticallyAccept = false // Disable video acceptance
            coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
            coreContext.core.isVideoDisplayEnabled = false
            updateVideoActivationPolicy(false)
            Log.i("Dialer","[Dialer] video enable after disabling: ${coreContext.core.isVideoEnabled}")
            Log.i("Dialer","[Dialer] SIP Address - $addressToCall")
            coreContext.startCall(addressToCall)
            eraseAll()
        } else {
            setLastOutgoingCallAddress()
        }
    }

    fun startVideoCall() {
        val addressToCall = enteredUri.value.orEmpty()
        if (addressToCall.isNotEmpty()) {
            // Enable video for the call
            Log.i("Dialer","[Dialer] video enable: ${coreContext.core.isVideoEnabled}")

           // coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
          //  coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
            coreContext.core.isVideoCaptureEnabled = true // Enable video capture
            coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
            updateVideoActivationPolicy(true)
            Log.i("Dialer","[Dialer] video enable after enabling:  ${coreContext.core.isVideoEnabled}")
            // coreContext.switchCamera()
            coreContext.startCall(addressToCall)
            eraseAll()
        } else {
            setLastOutgoingCallAddress()
        }
    }

    fun transferCallTo(addressToCall: String) {
        if (!coreContext.transferCallTo(addressToCall)) {
            //onMessageToNotifyEvent.value = Event(org.linphone.R.string.dialer_transfer_failed)
        }
    }

    fun switchCamera() {
        coreContext.switchCamera()
    }

    private fun setLastOutgoingCallAddress() {
        val callLog = coreContext.core.lastOutgoingCallLog
        if (callLog != null) {
            enteredUri.value = LinphoneUtils.getDisplayableAddress(callLog.remoteAddress)
        }
    }
}
