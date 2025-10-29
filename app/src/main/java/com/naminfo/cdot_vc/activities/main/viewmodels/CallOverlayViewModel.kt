package com.naminfo.cdot_vc.activities.main.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.tools.Log

class CallOverlayViewModel : ViewModel() {
    val displayCallOverlay = MutableLiveData<Boolean>()

    private val listener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            if (core.callsNb == 1 && call.state == Call.State.Connected) {
                Log.i("[Call Overlay] First call connected, creating it")
                createCallOverlay()
            }
        }

        override fun onLastCallEnded(core: Core) {
            Log.i("[Call Overlay] Last call ended, removing it")
            removeCallOverlay()
        }
    }

    init {
        displayCallOverlay.value = corePreferences.showCallOverlay &&
            !corePreferences.systemWideCallOverlay &&
            coreContext.core.callsNb > 0

        coreContext.core.addListener(listener)
    }

    override fun onCleared() {
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    private fun createCallOverlay() {
        // If overlay is disabled or if system-wide call overlay is enabled, abort
        if (!corePreferences.showCallOverlay || corePreferences.systemWideCallOverlay) {
            return
        }

        displayCallOverlay.value = true
    }

    private fun removeCallOverlay() {
        displayCallOverlay.value = false
    }
}
