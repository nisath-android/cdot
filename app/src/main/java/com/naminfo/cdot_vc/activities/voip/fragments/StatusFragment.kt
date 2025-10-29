package com.naminfo.cdot_vc.activities.voip.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import java.util.*
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.ControlsViewModel
import com.naminfo.cdot_vc.activities.voip.viewmodels.StatusViewModel
import org.linphone.core.Call
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.databinding.VoipStatusFragmentBinding
import com.naminfo.cdot_vc.utils.DialogUtils

class StatusFragment : GenericFragment<VoipStatusFragmentBinding>() {
    private lateinit var viewModel: StatusViewModel
    private val controlsViewModel: ControlsViewModel by navGraphViewModels(R.id.call_nav_graph)

    private var zrtpDialog: Dialog? = null

    override fun getLayoutId(): Int = R.layout.voip_status_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        useMaterialSharedAxisXForwardAnimation = false

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]
        binding.viewModel = viewModel

        binding.setRefreshClickListener {
            viewModel.refreshRegister()
        }

        viewModel.showZrtpDialogEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { call ->
                if (call.state == Call.State.Connected || call.state == Call.State.StreamsRunning) {
                    showZrtpDialog(call)
                }
            }
        }

        viewModel.showCallStatsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                controlsViewModel.showCallStats(skipAnimation = true)
            }
        }
    }

    override fun onDestroy() {
        if (zrtpDialog != null) {
            zrtpDialog?.dismiss()
        }
        super.onDestroy()
    }

    private fun showZrtpDialog(call: Call) {
        if (zrtpDialog != null && zrtpDialog?.isShowing == true) {
            Log.w(
                "[Status Fragment] ZRTP dialog already visible, closing it and creating a new one"
            )
            zrtpDialog?.dismiss()
            zrtpDialog = null
        }

        val token = call.authenticationToken
        if (token == null || token.length < 4) {
            Log.e("[Status Fragment] ZRTP token is invalid: $token")
            return
        }

        val toRead: String
        val toListen: String
        when (call.dir) {
            Call.Dir.Incoming -> {
                toRead = token.substring(0, 2)
                toListen = token.substring(2)
            }
            else -> {
                toRead = token.substring(2)
                toListen = token.substring(0, 2)
            }
        }

        val viewModel = DialogViewModel(
            getString(R.string.zrtp_dialog_explanation),
            getString(R.string.zrtp_dialog_title)
        )
        viewModel.showZrtp = true
        viewModel.zrtpReadSas = toRead.uppercase(Locale.getDefault())
        viewModel.zrtpListenSas = toListen.uppercase(Locale.getDefault())
        viewModel.showIcon = true
        viewModel.iconResource = if (call.audioStats?.isZrtpKeyAgreementAlgoPostQuantum == true) {
            R.drawable.security_post_quantum
        } else {
            R.drawable.security_2_indicator
        }

        val dialog: Dialog = DialogUtils.getVoipDialog(requireContext(), viewModel)

        viewModel.showCancelButton(
            {
                if (call.state != Call.State.End && call.state != Call.State.Released) {
                    if (call.authenticationTokenVerified) {
                        Log.w(
                            "[Status Fragment] Removing trust from previously verified ZRTP SAS auth token"
                        )
                        this@StatusFragment.viewModel.previouslyDeclineToken = true
                        call.authenticationTokenVerified = false
                    }
                } else {
                    Log.e(
                        "[Status Fragment] Can't decline the ZRTP SAS token, call is in state [${call.state}]"
                    )
                }
                dialog.dismiss()
                zrtpDialog = null
            },
            getString(R.string.zrtp_dialog_later_button_label)
        )

        viewModel.showOkButton(
            {
                if (call.state != Call.State.End && call.state != Call.State.Released) {
                    call.authenticationTokenVerified = true
                } else {
                    Log.e(
                        "[Status Fragment] Can't verify the ZRTP SAS token, call is in state [${call.state}]"
                    )
                }
                dialog.dismiss()
                zrtpDialog = null
            },
            getString(R.string.zrtp_dialog_correct_button_label)
        )

        viewModel.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        zrtpDialog = dialog
        dialog.show()
    }
}
