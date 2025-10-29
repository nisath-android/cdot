package com.naminfo.cdot_vc.activities.main.contact.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.naminfo.cdot_vc.R
//import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.*
import com.naminfo.cdot_vc.activities.main.*
import com.naminfo.cdot_vc.activities.main.contact.data.CallTypeWithPhoneNumber
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.ContactViewModel
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.ContactViewModelFactory
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
//import com.naminfo.cdot_vc.activities.navigateToChatRoom
import com.naminfo.cdot_vc.activities.navigateToDialer
import com.naminfo.cdot_vc.databinding.FragmentDetailContactBinding
import com.naminfo.cdot_vc.utils.DialogUtils
import com.naminfo.cdot_vc.utils.Event


class DetailContactFragment  : GenericFragment<FragmentDetailContactBinding>() {
    private lateinit var viewModel: ContactViewModel

    override fun getLayoutId(): Int = R.layout.fragment_detail_contact

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        binding.lifecycleOwner = viewLifecycleOwner

        binding.sharedMainViewModel = sharedViewModel

        useMaterialSharedAxisXForwardAnimation = sharedViewModel.isSlidingPaneSlideable.value == false

        val id = arguments?.getString("id")
        val name = arguments?.getString("name")
        val number = arguments?.getString("number")
        Log.i("CDOT_VC","[Contact] Found id parameter in arguments id: $id, name: $name, number: $number")
        arguments?.clear()
        if (id != null) {
            Log.i("CDOT_VC","[Contact] Found contact id parameter in arguments: $id")
            sharedViewModel.selectedContact.value = coreContext.contactsManager.findContactById(id)
        }

        val contact = sharedViewModel.selectedContact.value
        if (contact == null) {
            Log.i("CDOT_VC","[Contact] Friend is null, aborting!")
            goBack()
            return
        }

        viewModel = ViewModelProvider(
            this,
            ContactViewModelFactory(contact)
        )[ContactViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.sendSmsToEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { number ->
                sendSms(number)
            }
        }

        viewModel.startCallToEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { address ->
                Log.i("CDOT_VC","[Contact] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Disable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = false // Disable video acceptance
                coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
                coreContext.core.isVideoDisplayEnabled = false
                updateVideoActivationPolicy(true)
                Log.i("CDOT_VC",
                    "[Contact] video enable after disabling: ${coreContext.core.isVideoEnabled}"
                )
                if (coreContext.core.callsNb > 0) {
                    Log.i("CDOT_VC",
                        "[Contact] Starting dialer with pre-filled URI ${address.asStringUriOnly()}, is transfer? ${sharedViewModel.pendingCallTransfer}"
                    )
                    sharedViewModel.updateContactsAnimationsBasedOnDestination.value =
                        Event(R.id.dialerFragment)
                    sharedViewModel.updateDialerAnimationsBasedOnDestination.value =
                        Event(R.id.masterContactsFragment)

                    val args = Bundle()
                    args.putString("URI", address.asStringUriOnly())
                    args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                    args.putBoolean(
                        "SkipAutoCallStart",
                        true
                    ) // If auto start call setting is enabled, ignore it
                    navigateToDialer(args)
                } else {
                    coreContext.startCall(address)
                    /*if (address.username != null) {
                        showCustomDialog(
                            address.username ?: "",
                            address.displayName ?: "",
                            address,
                            false
                        )
                    }*/
                }
            }
        }

        viewModel.startVideoCallToEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { address ->
                Log.i("CDOT_VC","[xxxContact] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
                updateVideoActivationPolicy(true)

                Log.i("CDOT_VC",
                    "[xxxContact] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                )
                if (coreContext.core.callsNb > 0) {
                    Log.i("CDOT_VC",
                        "[xxxContact] Starting dialer with pre-filled URI ${address.asStringUriOnly()}, is transfer? ${sharedViewModel.pendingCallTransfer}"
                    )
                    sharedViewModel.updateContactsAnimationsBasedOnDestination.value =
                        Event(R.id.dialerFragment)
                    sharedViewModel.updateDialerAnimationsBasedOnDestination.value =
                        Event(R.id.masterContactsFragment)

                    val args = Bundle()
                    args.putString("URI", address.asStringUriOnly())
                    args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                    args.putBoolean(
                        "SkipAutoCallStart",
                        true
                    ) // If auto start call setting is enabled, ignore it
                    navigateToDialer(args)
                } else {
                    coreContext.startCall(address)
                }
            }
        }

        binding.setDeleteClickListener {
            confirmContactRemoval()
        }

        viewModel.onMessageToNotifyEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        }
        viewModel.updateNumbersAndAddresses()

        startPostponedEnterTransition()
    }

    private fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }

    override fun onResume() {
        super.onResume()
        if (this::viewModel.isInitialized) {
            viewModel.registerContactListener()
            coreContext.contactsManager.contactIdToWatchFor = viewModel.contact.value?.refKey ?: ""
        }
    }

    override fun onPause() {
        super.onPause()
        coreContext.contactsManager.contactIdToWatchFor = ""
        if (this::viewModel.isInitialized) {
            viewModel.unregisterContactListener()
        }
    }

    private fun confirmContactRemoval() {
        val dialogViewModel = DialogViewModel(getString(R.string.contact_delete_one_dialog))
        dialogViewModel.showIcon = true
        dialogViewModel.iconResource = R.drawable.dialog_delete_icon
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialogViewModel.showDeleteButton(
            {
                viewModel.deleteContact()
                dialog.dismiss()
                goBack()
            },
            getString(R.string.dialog_delete)
        )

        dialog.show()
    }

    private fun sendSms(number: String) {
        val smsIntent = Intent(Intent.ACTION_SENDTO)
        smsIntent.putExtra("address", number)
        smsIntent.data = Uri.parse("smsto:$number")
        val text = getString(R.string.contact_send_sms_invite_text).format(
            getString(R.string.contact_send_sms_invite_download_link)
        )
        smsIntent.putExtra("sms_body", text)
        startActivity(smsIntent)
    }

}
