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
package org.naminfo.activities.main.history.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.isDigitsOnly
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import org.linphone.core.Address
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.*
import org.naminfo.activities.main.*
import org.naminfo.activities.main.contact.adapters.OptionsAdapter.Companion.splitContacts
import org.naminfo.activities.main.contact.data.GroupSettingsContact
import org.naminfo.activities.main.history.data.GroupedCallLogData
import org.naminfo.activities.main.history.viewmodels.CallLogViewModel
import org.naminfo.activities.navigateToContacts
import org.naminfo.databinding.HistoryDetailFragmentBinding
import org.naminfo.utils.Event

class DetailCallLogFragment : GenericFragment<HistoryDetailFragmentBinding>() {
    private lateinit var viewModel: CallLogViewModel
    companion object {
        var clickTimeCallType: String? = null
        var groupSetting: GroupSettingsContact? = null
        data class TempAdapter(
            var username: String,
            var phone: String,
            var callTypeLocal: String,
            var callTypeValue: Pair<String, String>
        )
    }
    override fun getLayoutId(): Int = R.layout.history_detail_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.sharedMainViewModel = sharedViewModel

        val callLogGroup = sharedViewModel.selectedCallLogGroup.value
        if (callLogGroup == null) {
            Log.e("[xxxHistory] Call log group is null, aborting!")
            findNavController().navigateUp()
            return
        }

        viewModel = callLogGroup.lastCallLogViewModel
        binding.viewModel = viewModel

        updateCallType(callLogGroup)
        if (viewModel.relatedCallLogs.value.orEmpty().isEmpty()) {
            viewModel.addRelatedCallLogs(callLogGroup.callLogs)
        }

        useMaterialSharedAxisXForwardAnimation =
            sharedViewModel.isSlidingPaneSlideable.value == false

        binding.setNewContactClickListener {
            val copy = viewModel.callLog.remoteAddress.clone()
            copy.clean()
            val address = copy.asStringUriOnly()
            Log.i("[History] Creating contact with SIP URI [$address]")
            sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                R.id.masterCallLogsFragment
            )
            navigateToContacts(address)
        }

        binding.setContactClickListener {
            sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                R.id.masterCallLogsFragment
            )
            val contactId = viewModel.contact.value?.refKey
            if (contactId != null) {
                Log.i("[History] Displaying native contact [$contactId]")
                navigateToNativeContact(contactId)
            } else {
                val copy = viewModel.callLog.remoteAddress.clone()
                copy.clean()
                val address = copy.asStringUriOnly()
                Log.i("[History] Displaying friend with address [$address]")
                navigateToFriend(address)
            }
        }

        viewModel.startCallEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { callLog ->
                Log.i("[History] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = false // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = false // Enable video capture
                coreContext.core.isVideoDisplayEnabled = false // Ensure video display is enabled
                Log.i(
                    "[History] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                )
                updateVideoActivationPolicy(true)
                // To remove the GRUU if any
                val address = callLog.remoteAddress.clone()
                address.clean()
                Log.i("[History] In start call event")

                if (coreContext.core.callsNb > 0) {
                    Log.i(
                        "[History] Starting dialer with pre-filled URI [${address.asStringUriOnly()}], is transfer? ${sharedViewModel.pendingCallTransfer}"
                    )
                    sharedViewModel.updateDialerAnimationsBasedOnDestination.value =
                        Event(R.id.masterCallLogsFragment)

                    val args = Bundle()
                    args.putString("URI", address.asStringUriOnly())
                    args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                    args.putBoolean(
                        "SkipAutoCallStart",
                        true
                    ) // If auto start call setting is enabled, ignore it
                    navigateToDialer(args)
                } else {
                    val localAddress = callLog.localAddress
                    Log.i(
                        "[History] Starting call to ${address.asStringUriOnly()} with local address ${localAddress.asStringUriOnly()}"
                    )

                    coreContext.startCall(address, localAddress = localAddress)
                }
            }
        }

        viewModel.startVideoCallEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { callLog ->
                Log.i("[History] video enable: ${coreContext.core.isVideoEnabled}")
                coreContext.core.videoActivationPolicy.automaticallyInitiate = true // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
                Log.i(
                    "[History] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                )
                updateVideoActivationPolicy(true)
                // To remove the GRUU if any
                val address = callLog.remoteAddress.clone()
                address.clean()
                Log.i("[History] In start video call event")

                if (coreContext.core.callsNb > 0) {
                    Log.i(
                        "[History] Starting dialer with pre-filled URI [${address.asStringUriOnly()}], is transfer? ${sharedViewModel.pendingCallTransfer}"
                    )
                    sharedViewModel.updateDialerAnimationsBasedOnDestination.value =
                        Event(R.id.masterCallLogsFragment)

                    val args = Bundle()
                    args.putString("URI", address.asStringUriOnly())
                    args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                    args.putBoolean(
                        "SkipAutoCallStart",
                        true
                    ) // If auto start call setting is enabled, ignore it

                    navigateToDialer(args)
                } else {
                    val localAddress = callLog.localAddress
                    Log.i(
                        "[History] Starting video call to ${address.asStringUriOnly()} with local address ${localAddress.asStringUriOnly()}"
                    )
                    coreContext.startCall(address, localAddress = localAddress)
                }
            }
        }

        viewModel.chatRoomCreatedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatRoom ->
                val args = Bundle()
                args.putString("LocalSipUri", chatRoom.localAddress.asStringUriOnly())
                args.putString("RemoteSipUri", chatRoom.peerAddress.asStringUriOnly())
                navigateToChatRoom(args)
            }
        }

        viewModel.onMessageToNotifyEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        }
    }

    private fun updateCallType(callLogGroup: GroupedCallLogData) {
        Log.i(
            "[CallLogs-DetailCall]",
            " Before updateCallType => calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
        )
        when (callLogGroup.callType) {
            "PBX" -> {
                // Handle PBX case
                viewModel.isClickableMsg.value = false
                viewModel.isClickableVideo.value = false
                viewModel.isClickableAudio.value = true

                viewModel.callType.value = Pair("pbx", "audio")
                callLogGroup.callType = "PBX"
                Log.i(
                    "[CallLogs-DetailCall]",
                    "19- After callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )
            }

            "Group" -> {
                // Handle Group case
                viewModel.isClickableMsg.value = false
                Log.i(
                    "[CallLogs-DetailCall]",
                    " 20=> moderate=${callLogGroup.groupSettingContact?.moderate}\n" +
                        "current user=${corePreferences.getCurrentUserPhoneNumber}\n" +
                        "group number=${callLogGroup.groupSettingContact?.groupNumber}\n" +
                        "group callType=${callLogGroup.groupSettingContact?.callType},\n" +
                        "conference=${callLogGroup.groupSettingContact?.conference}"
                )
                if (callLogGroup.groupSettingContact != null) {
                    if (callLogGroup.groupSettingContact?.callType == "Dial-OUT") {
                        splitContacts(
                            callLogGroup.groupSettingContact?.moderate
                                ?: "${callLogGroup.groupSettingContact?.groupName}-${callLogGroup.groupSettingContact?.groupNumber}"
                        ).forEach { (name, number) ->
                            if (corePreferences.getCurrentUserPhoneNumber.toString()
                                .equals(number)
                            ) {
                                Log.i("[CallLogs-DetailCall]", "Current user")

                                if (callLogGroup.groupSettingContact?.conference == "Video") {
                                    viewModel.isClickableAudio.value = true
                                    viewModel.isClickableVideo.value = true
                                }
                                if (callLogGroup.groupSettingContact?.conference == "Audio") {
                                    viewModel.isClickableAudio.value = true
                                    viewModel.isClickableVideo.value = false
                                }
                            } else {
                                Log.i("[CallLogs-DetailCall]", "Not Current user")
                                viewModel.isClickableAudio.value = false
                                viewModel.isClickableVideo.value = false
                            }
                        }
                        viewModel.callType.value = Pair(
                            "group",
                            "${callLogGroup.groupSettingContact?.callType}-${callLogGroup.groupSettingContact?.conference}"
                        )

                        Log.i(
                            "[CallLogs-DetailCall]",
                            "21=> Dial-OUT callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                        )
                    } else if (callLogGroup.groupSettingContact?.callType == "Dial-IN") {
                        if (callLogGroup.groupSettingContact?.conference == "Video") {
                            viewModel.isClickableAudio.value = true
                            viewModel.isClickableVideo.value = true
                        }
                        if (callLogGroup.groupSettingContact?.conference == "Audio") {
                            viewModel.isClickableAudio.value = true
                            viewModel.isClickableVideo.value = false
                        }
                        viewModel.callType.value = Pair(
                            "group",
                            "${
                            callLogGroup.groupSettingContact?.callType?.replace(
                                "-",
                                ""
                            )
                            }-${callLogGroup.groupSettingContact?.conference}"
                        )
                        Log.i(
                            "[CallLogs-DetailCall]",
                            "22=> Dial-OUT callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                        )
                    } else {
                        Log.i(
                            "[CallLogs-DetailCall]",
                            "${callLogGroup.groupSettingContact?.callType}"
                        )
                        viewModel.isClickableAudio.value = false
                        viewModel.isClickableVideo.value = false
                        viewModel.callType.value = Pair(
                            "group",
                            "unknown-${callLogGroup.groupSettingContact?.conference}"
                        )
                        Log.i(
                            "[CallLogs-DetailCall]",
                            "23=> Else-Dial-OUT callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                        )
                    }
                } else {
                    val (first, second) = viewModel.callType.value ?: Pair("", "")
                    when (first) {
                        "pbx" -> {}
                        "group" -> {
                            if (second?.isDigitsOnly() == true) {
                                val number = second.toLongOrNull()
                                if (number != null) {
                                    when {
                                        number in 3000..3499 -> {
                                            viewModel.apply {
                                                isClickableMsg.value = false
                                                isClickableVideo.value = true
                                                isClickableAudio.value = true
                                                callType.value = Pair("group", number.toString())
                                            }
                                            callLogGroup.callType = "Group"
                                            Log.i(
                                                "[CallLogs-DetailCall]",
                                                "61=>username=> $second | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                            )
                                        }

                                        number in 3500..3999 -> {
                                            viewModel.apply {
                                                isClickableMsg.value = false
                                                isClickableVideo.value = true
                                                isClickableAudio.value = true
                                                callType.value = Pair("group", number.toString())
                                            }
                                            callLogGroup.callType = "Group"
                                            Log.i(
                                                "[CallLogs-DetailCall]",
                                                "62=>username=> $second | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                            )
                                        }

                                        number in 5000..5999 -> {
                                            viewModel.apply {
                                                isClickableMsg.value = false
                                                isClickableVideo.value = false
                                                isClickableAudio.value = true
                                                callType.value = Pair("group", number.toString())
                                            }
                                            callLogGroup.callType = "Group"
                                            Log.i(
                                                "[CallLogs-DetailCall]",
                                                "63=>username=> $second | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                            )
                                        }

                                        number != null && second.length >= 10 -> {
                                            viewModel.apply {
                                                isClickableMsg.value = false
                                                isClickableVideo.value = false
                                                isClickableAudio.value = false
                                                callType.value = Pair("gsm", second)
                                            }
                                            callLogGroup.callType = "11-Contact"
                                            Log.i(
                                                "[CallLogs-DetailCall]",
                                                "64=>username=> $second | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                            )
                                        }

                                        else -> {
                                            Log.i(
                                                "[CallLogs-DetailCall]",
                                                "65=>Else -username=> $second"
                                            )
                                            resetViewModel(callLogGroup)
                                        }
                                    }
                                } else {
                                    Log.i(
                                        "[CallLogs-DetailCall]",
                                        "66-Else -username=> $second"
                                    )
                                    resetViewModel(callLogGroup)
                                }
                            } else {
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "67=> Else -username=> $second"
                                )
                                resetViewModel(callLogGroup)
                            }
                        }
                        "web" -> {}
                        "gsm" -> {}
                        else -> {}
                    }
                }

                callLogGroup.callType = "Group"
                Log.i(
                    "[CallLogs-DetailCall]",
                    "24=>confrence not match dial-in/out callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )
            }

            "00-Contact" -> {
                // Handle 00-Contact case MobiWeb
                viewModel.isClickableMsg.value = false
                viewModel.isClickableVideo.value = true
                viewModel.isClickableAudio.value = true
                viewModel.callType.value = Pair("web", "audio-video")

                callLogGroup.callType = "00-Contact"
                Log.i(
                    "[CallLogs-DetailCall]",
                    "25=>callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )
            }

            "11-Contact" -> {
                // Handle 11-Contact case Gsm Call
                viewModel.isClickableMsg.value = false
                viewModel.isClickableVideo.value = false
                viewModel.isClickableAudio.value = true
                viewModel.callType.value = Pair("gsm", "audio")
                callLogGroup.callType = "11-Contact"
                Log.i(
                    "[CallLogs-DetailCall]",
                    "26=>callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )
            }

            "Contact" -> {
                // Handle Contact case
                viewModel.isClickableMsg.value = true
                viewModel.isClickableVideo.value = true
                viewModel.isClickableAudio.value = true
                viewModel.callType.value = Pair("", "audio-video")
                callLogGroup.callType = "Contact"
                Log.i(
                    "[CallLogs-DetailCall]",
                    "27=>callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )
            }

            else -> {
                // Handle default case
                Log.i(
                    "[CallLogs-DetailCall]",
                    "28=>Else -callLogGroup.calltype: ${callLogGroup.callType} || callType.value: ${viewModel.callType.value}"
                )

                Log.i(
                    "[CallLogs-DetailCall]",
                    "29=>${callLogGroup.lastCallLog.remoteAddress.username}\n" +
                        "${callLogGroup.lastCallLog.remoteAddress.displayName}\n" +
                        "${callLogGroup.lastCallLogViewModel.callLog.remoteAddress.username}\n" +
                        "${callLogGroup.lastCallLogViewModel.callLog.remoteAddress.displayName}\n"
                )
                var username = callLogGroup.lastCallLog.remoteAddress.username ?: viewModel.dialPadNumber.value ?: corePreferences.typeOfCall
                Log.i(
                    "[CallLogs-DetailCall]",
                    "30=>Else -username=> $username"
                )
                if ((username.isNullOrEmpty() || username == "null") && corePreferences.typeOfCall.isNullOrEmpty()) {
                    username = corePreferences.typeOfCall
                } else {
                    username = clickTimeCallType
                }
                if (username?.isDigitsOnly() == true) {
                    val number = username.toLongOrNull()
                    if (number != null) {
                        when {
                            number in 3000..3499 -> {
                                viewModel.apply {
                                    isClickableMsg.value = false
                                    isClickableVideo.value = true
                                    isClickableAudio.value = true
                                    callType.value = Pair("group", number.toString())
                                }
                                callLogGroup.callType = "Group"
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "31=>username=> $username | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                )
                            }

                            number in 3500..3999 -> {
                                viewModel.apply {
                                    isClickableMsg.value = false
                                    isClickableVideo.value = true
                                    isClickableAudio.value = true
                                    callType.value = Pair("group", number.toString())
                                }
                                callLogGroup.callType = "Group"
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "32=>username=> $username | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                )
                            }

                            number in 5000..5999 -> {
                                viewModel.apply {
                                    isClickableMsg.value = false
                                    isClickableVideo.value = false
                                    isClickableAudio.value = true
                                    callType.value = Pair("group", number.toString())
                                }
                                callLogGroup.callType = "Group"
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "33=>username=> $username | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                )
                            }

                            number != null && username.length >= 10 -> {
                                viewModel.apply {
                                    isClickableMsg.value = false
                                    isClickableVideo.value = false
                                    isClickableAudio.value = false
                                    callType.value = Pair("gsm", username)
                                }
                                callLogGroup.callType = "11-Contact"
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "34=>username=> $username | callLogGroup.callType:${callLogGroup.callType}| callType.value:${viewModel.callType.value}"
                                )
                            }

                            else -> {
                                Log.i(
                                    "[CallLogs-DetailCall]",
                                    "35=>Else -username=> $username"
                                )
                                resetViewModel(callLogGroup)
                            }
                        }
                    } else {
                        Log.i(
                            "[CallLogs-DetailCall]",
                            "36-Else -username=> $username"
                        )
                        resetViewModel(callLogGroup)
                    }
                } else {
                    Log.i(
                        "[CallLogs-DetailCall]",
                        "37=> Else -username=> $username"
                    )
                    resetViewModel(callLogGroup)
                }
            }
        }
    }
    fun logs(logid: String, delimeter: String, msgTitle: String, value: String) {
        android.util.Log.i(
            logid,
            "$msgTitle:$delimeter$value"
        )
    }

    // Function to reset ViewModel values
    fun resetViewModel(callLogGroup: GroupedCallLogData) {
        viewModel.apply {
            callType.value = Pair("", "")
            isClickableMsg.value = false
            isClickableVideo.value = false
            isClickableAudio.value = false
        }
        corePreferences.typeOfCall = ""
        callLogGroup.callType = ""
    }

    private fun showCustomDialog(
        phoneAddress: String,
        remoteAddress: Address,
        localAddress: Address,
        isVideo: Boolean = false,
        callType: String = ""
    ) {
        Log.i(
            "[CallLogs-DetailCall] DetailCallLogFragment-In showCustomDialog = $phoneAddress  , $isVideo"
        )
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
                coreContext.core.videoActivationPolicy.automaticallyInitiate =
                    true // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept =
                    true // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
                updateVideoActivationPolicy(true)
                mobiFSCallBTN.setText("Video")
                gsmCallBTN.visibility = View.GONE
            } else {
                coreContext.core.videoActivationPolicy.automaticallyInitiate =
                    false // Enable video initiation
                coreContext.core.videoActivationPolicy.automaticallyAccept =
                    false // Enable video acceptance
                coreContext.core.isVideoCaptureEnabled = false // Enable video capture
                coreContext.core.isVideoDisplayEnabled = false // Ensure video display is enabled
                updateVideoActivationPolicy(true)
                mobiFSCallBTN.setText("Audio")
            }
            var phoneNumberModified = ""
            corePreferences.getRemoteUserPhoneNumber = phoneAddress.trim().takeLast(10)
            mobiFSCallBTN.setOnClickListener {
                phoneNumberModified = phoneAddress.trim().takeLast(10)
                /* val username = remoteAddress.username
                 val displayName = remoteAddress.displayName
                 remoteAddress.setUsername(phoneNumberModified)
                 remoteAddress.setDisplayName(displayName)*/
                remoteAddress.setUsername(phoneNumberModified)
                Log.i(
                    "[CallLogs-DetailCall] -Phone address mobiFSCallBTN-> $phoneNumberModified remoteAddress:${remoteAddress.asStringUriOnly()} ,localAddress:${localAddress.asStringUriOnly()}"
                )
                coreContext.startCall(remoteAddress, localAddress = localAddress)
                dialog.dismiss()
            }
            gsmCallBTN.setOnClickListener {
                phoneNumberModified = "11${phoneAddress.trim().takeLast(10)}"
                val username = remoteAddress.username
                val displayName = remoteAddress.displayName
                remoteAddress.setUsername(phoneNumberModified)

                // val sipUriModified = address.asStringUriOnly()
                // address?.setUriParams(modifySipUri(sipUriModified, "11"))
                Log.i(
                    "[CallLogs-DetailCall] DetailCallLogFragment-Phone address gsmCallBTN-> username:$username,displayName:$displayName,phoneNumberModified:$phoneNumberModified sip-uri=${remoteAddress.asStringUriOnly()}"
                )
                coreContext.startCall(remoteAddress, localAddress = localAddress)
                dialog.dismiss()
            }
            mobiWebCallBTN.setOnClickListener {
                phoneNumberModified = "00${phoneAddress.trim().takeLast(10)}"

                remoteAddress.setUsername(phoneNumberModified)

                Log.i(
                    "[CallLogs-DetailCall] DetailCallLogFragment-Phone address mobiWebCallBTN-> $phoneNumberModified sip-uri=${remoteAddress.asStringUriOnly()}"
                )
                coreContext.startCall(remoteAddress, localAddress = localAddress)
                dialog.dismiss()
            }
            closeDialog.setOnClickListener {
                phoneNumberModified = ""
                corePreferences.getRemoteUserPhoneNumber = ""
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    fun modifySipUri(sipUri: String, prefixCode: String): String {
        val regex = Regex("sip:(\\d+)@(.*)")
        return sipUri.replace(regex) { matchResult ->
            val phoneNumber = matchResult.groupValues[1]
            val domain = matchResult.groupValues[2]
            Log.i("[xxxyyy] phoneNumber: $phoneNumber, domain: $domain")
            "sip:$prefixCode$phoneNumber%40$domain"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.enableListener(true)
    }

    override fun onPause() {
        viewModel.enableListener(false)
        super.onPause()
    }

    fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }
}
