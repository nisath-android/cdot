/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.naminfo.activities.main.contact.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.*
import org.naminfo.activities.main.*
import org.naminfo.activities.main.contact.data.CallTypeWithPhoneNumber
import org.naminfo.activities.main.contact.viewmodels.ContactViewModel
import org.naminfo.activities.main.contact.viewmodels.ContactViewModelFactory
import org.naminfo.activities.main.viewmodels.DialogViewModel
import org.naminfo.activities.navigateToChatRoom
import org.naminfo.activities.navigateToDialer
import org.naminfo.databinding.ContactDetailFragmentBinding
import org.naminfo.utils.DialogUtils
import org.naminfo.utils.Event

class DetailContactFragment : GenericFragment<ContactDetailFragmentBinding>() {
    private lateinit var viewModel: ContactViewModel

    override fun getLayoutId(): Int = R.layout.contact_detail_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        binding.lifecycleOwner = viewLifecycleOwner

        binding.sharedMainViewModel = sharedViewModel

        useMaterialSharedAxisXForwardAnimation = sharedViewModel.isSlidingPaneSlideable.value == false

        val id = arguments?.getString("id")
        val name = arguments?.getString("name")
        val number = arguments?.getString("number")
        Log.i("[Contact] Found id parameter in arguments id: $id, name: $name, number: $number")
        arguments?.clear()
        if (id != null) {
            Log.i("[Contact] Found contact id parameter in arguments: $id")
            sharedViewModel.selectedContact.value = coreContext.contactsManager.findContactById(id)
        }

        val contact = sharedViewModel.selectedContact.value
        if (contact == null) {
            Log.e("[Contact] Friend is null, aborting!")
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
                Log.i("[Contact] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Disable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = false // Disable video acceptance
                coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
                coreContext.core.isVideoDisplayEnabled = false
                coreContext.core.currentCall?.currentParams?.isVideoEnabled = false
                updateVideoActivationPolicy(true)
                Log.i(
                    "[Contact] video enable after disabling: ${coreContext.core.isVideoEnabled}"
                )
                if (coreContext.core.callsNb > 0) {
                    Log.i(
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
                Log.i("[xxxContact] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
                coreContext.core.currentCall?.currentParams?.isVideoEnabled = true
                updateVideoActivationPolicy(true)

                Log.i(
                    "[xxxContact] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                )
                if (coreContext.core.callsNb > 0) {
                    Log.i(
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
                    /*if (address.username != null) {
                        showCustomDialog(
                            address.username ?: "",
                            address.displayName ?: "",
                            address,
                            true
                        )
                    }*/
                    coreContext.startCall(address)
                }
            }
        }

        viewModel.chatRoomCreatedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatRoom ->
                sharedViewModel.updateContactsAnimationsBasedOnDestination.value =
                    Event(R.id.masterChatRoomsFragment)
                val args = Bundle()
                args.putString("LocalSipUri", chatRoom.localAddress.asStringUriOnly())
                args.putString("RemoteSipUri", chatRoom.peerAddress.asStringUriOnly())
                navigateToChatRoom(args)
            }
        }

        /*binding.setEditClickListener {
            navigateToContactEditor()
        }*/

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

    private fun showCustomDialog(
        phoneAddress: String,
        displayName: String,
        remoteAddress: org.linphone.core.Address,
        isVideo: Boolean = false
    ) {
        Log.i("[xxxyyyContact] showCustomDialog= $phoneAddress , $displayName , $isVideo")
        if (isVisible) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(
                R.layout.custom_call_dialog_screen,
                null
            )
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val mobiFSCallBTN = dialogView.findViewById<MaterialButton>(R.id.mobiFSCallBTN)
            val gsmCallBTN = dialogView.findViewById<MaterialButton>(R.id.gsmCallBTN)
            val mobiWebCallBTN = dialogView.findViewById<MaterialButton>(R.id.mobiWebCallBTN)
            val closeDialog = dialogView.findViewById<ImageView>(R.id.close_dialog)
            if (isVideo) {
                coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
                coreContext.core.currentCall?.currentParams?.isVideoEnabled = true
                updateVideoActivationPolicy(true)
                mobiFSCallBTN.setText("Video")
                gsmCallBTN.visibility = View.GONE
            } else {
                coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = false // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = false // Enable video capture
                coreContext.core.isVideoDisplayEnabled = false // Ensure video display is enabled
                coreContext.core.currentCall?.currentParams?.isVideoEnabled = false
                updateVideoActivationPolicy(true)
                mobiFSCallBTN.setText("Audio")
            }
            var phoneNumberModified = ""
            //  var addressModified: org.linphone.core.Address? = address
            corePreferences.getRemoteUserPhoneNumber = phoneAddress.trim().takeLast(10)
            mobiFSCallBTN.setOnClickListener {
                phoneNumberModified = phoneAddress.trim().takeLast(10)
                Log.i(
                    "[xxxyyyContact] Phone address mobiFSCallBTN-> $phoneNumberModified userName=${remoteAddress.username},displayName=${remoteAddress.displayName},uri=${remoteAddress.asStringUriOnly()}"
                )
                if (phoneNumberModified.isNotEmpty()) {
                    remoteAddress?.setUsername(phoneNumberModified)
                }
                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "mobiFSCallBTN"
                        )
                    )
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "mobiFSCallBTN"
                        )
                    )
                }
                coreContext.startCall(remoteAddress)
                phoneNumberModified = ""

                dialog.dismiss()
            }
            gsmCallBTN.setOnClickListener {
                val phone = phoneAddress.trim().takeLast(10)
                phoneNumberModified = "11$phone"
                remoteAddress?.setUsername(phoneNumberModified)
                if (remoteAddress.displayName == null) {
                    remoteAddress?.setDisplayName(displayName)
                }
                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "gsmCallBTN"
                        )
                    )
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "gsmCallBTN"
                        )
                    )
                }
                Log.i(
                    "[xxxyyyContact] Phone address gsmCallBTN-> $phoneNumberModified sip-uri=${remoteAddress.asStringUriOnly()}"
                )

                coreContext.startCall(remoteAddress!!)
                phoneNumberModified = ""
                dialog.dismiss()
            }
            mobiWebCallBTN.setOnClickListener {
                val phone = phoneAddress.trim().takeLast(10)
                phoneNumberModified = "00$phone"
                remoteAddress?.setUsername(phoneNumberModified)
                if (remoteAddress.displayName == null) {
                    remoteAddress?.setDisplayName(displayName)
                }
                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "mobiWebCallBTN"
                        )
                    )
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "mobiWebCallBTN"
                        )
                    )
                }
                Log.i(
                    "[xxxyyyContact] Phone address mobiWebCallBTN-> $phoneNumberModified sip-uri=${remoteAddress.asStringUriOnly()}"
                )
                coreContext.startCall(remoteAddress!!)
                phoneNumberModified = ""
                dialog.dismiss()
            }
            closeDialog.setOnClickListener {
                phoneNumberModified = ""
                corePreferences.getRemoteUserPhoneNumber = ""
                // remoteAddress = null
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}
