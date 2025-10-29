package com.naminfo.cdot_vc.activities.voip.fragments

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.navigation.navGraphViewModels
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.navigateToActiveCall
import com.naminfo.cdot_vc.activities.voip.viewmodels.CallsViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.ControlsViewModel
import com.naminfo.cdot_vc.databinding.FragmentIncomingCallVoipBinding
import org.linphone.core.tools.Log


class IncomingCallFragment: GenericFragment<FragmentIncomingCallVoipBinding>() {
    private val controlsViewModel: ControlsViewModel by navGraphViewModels(R.id.call_nav_graph)
    private val callsViewModel: CallsViewModel by navGraphViewModels(R.id.call_nav_graph)

    override fun getLayoutId(): Int = R.layout.fragment_incoming_call_voip

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.controlsViewModel = controlsViewModel

        binding.callsViewModel = callsViewModel

        callsViewModel.callConnectedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                navigateToActiveCall()
            }
        }

        callsViewModel.callEndedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                navigateToActiveCall()
            }
        }

        callsViewModel.currentCallData.observe(
            viewLifecycleOwner
        ) {
            if (it != null) {
                val timer = binding.root.findViewById<Chronometer>(R.id.incoming_call_timer)
                timer.base =
                    SystemClock.elapsedRealtime() - (1000 * it.call.duration) // Linphone timestamps are in seconds
                timer.start()
            }
        }

        val earlyMediaVideo = arguments?.getBoolean("earlyMediaVideo") ?: false
        if (earlyMediaVideo) {
            Log.i("[Incoming Call] Video early media detected, setting native window id")
            coreContext.core.nativeVideoWindowId = binding.remoteVideoSurface
        }
    }

    // We don't want the proximity sensor to turn screen OFF in this fragment
    override fun onResume() {
        super.onResume()
        controlsViewModel.forceDisableProximitySensor.value = true
    }

    override fun onPause() {
        controlsViewModel.forceDisableProximitySensor.value = false
        super.onPause()
    }
}
