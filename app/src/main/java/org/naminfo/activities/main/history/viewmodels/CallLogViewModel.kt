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
package org.naminfo.activities.main.history.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.launch
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.main.conference.data.ConferenceSchedulingParticipantData
import org.naminfo.contact.GenericContactViewModel
import org.naminfo.utils.AppUtils
import org.naminfo.utils.Event
import org.naminfo.utils.LinphoneUtils
import org.naminfo.utils.TimestampUtils

class CallLogViewModel(val callLog: CallLog, private val isRelated: Boolean = false) : GenericContactViewModel(
    callLog.remoteAddress
) {
    val peerSipUri: String by lazy {
        LinphoneUtils.getDisplayableAddress(callLog.remoteAddress)
    }
    var isClickableAudio = MutableLiveData<Boolean>(true)
    var isClickableVideo = MutableLiveData<Boolean>(true)
    var isClickableMsg = MutableLiveData<Boolean>(true)
    var callType = MutableLiveData<Pair<String, String>>(Pair("", ""))
    var dialPadNumber = MutableLiveData<String>("")

/*    val textColor: LiveData<Int> = Tra.map(callType) {
        when (it) {
            "pbx" -> R.color.green
            "group" -> R.color.blue
            "gsm" -> R.color.red
            else -> R.color.gray
        }
    }*/
    val statusIconResource: Int by lazy {
        if (callLog.dir == Call.Dir.Incoming) {
            if (LinphoneUtils.isCallLogMissed(callLog)) {
                R.drawable.call_status_missed
            } else {
                R.drawable.call_status_incoming
            }
        } else {
            R.drawable.call_status_outgoing
        }
    }

    val iconContentDescription: Int by lazy {
        if (callLog.dir == Call.Dir.Incoming) {
            if (LinphoneUtils.isCallLogMissed(callLog)) {
                R.string.content_description_missed_call
            } else {
                R.string.content_description_incoming_call
            }
        } else {
            R.string.content_description_outgoing_call
        }
    }

    val directionIconResource: Int by lazy {
        if (callLog.dir == Call.Dir.Incoming) {
            if (LinphoneUtils.isCallLogMissed(callLog)) {
                R.drawable.call_missed
            } else {
                R.drawable.call_incoming
            }
        } else {
            R.drawable.call_outgoing
        }
    }

    val duration: String by lazy {
        val dateFormat = SimpleDateFormat(
            if (callLog.duration >= 3600) "HH:mm:ss" else "mm:ss",
            Locale.getDefault()
        )
        val cal = Calendar.getInstance()
        cal[0, 0, 0, 0, 0] = callLog.duration
        dateFormat.format(cal.time)
    }

    val date: String by lazy {
        TimestampUtils.toString(callLog.startDate, shortDate = false, hideYear = false)
    }

    val startCallEvent: MutableLiveData<Event<CallLog>> by lazy {
        MutableLiveData<Event<CallLog>>()
    }

    val startVideoCallEvent: MutableLiveData<Event<CallLog>> by lazy {
        MutableLiveData<Event<CallLog>>()
    }

    val chatRoomCreatedEvent: MutableLiveData<Event<ChatRoom>> by lazy {
        MutableLiveData<Event<ChatRoom>>()
    }

    val waitForChatRoomCreation = MutableLiveData<Boolean>()

    val chatAllowed = !corePreferences.disableChat

    val hidePlainChat = corePreferences.forceEndToEndEncryptedChat

    val secureChatAllowed = LinphoneUtils.isEndToEndEncryptedChatAvailable() && (
        corePreferences.allowEndToEndEncryptedChatWithoutPresence || (
            contact.value?.getPresenceModelForUriOrTel(
                peerSipUri
            )?.hasCapability(Friend.Capability.LimeX3Dh) ?: false
            )
        )

    val relatedCallLogs = MutableLiveData<ArrayList<CallLogViewModel>>()

    private val listener = object : CoreListenerStub() {
        override fun onCallLogUpdated(core: Core, log: CallLog) {
            if (callLog.remoteAddress.weakEqual(log.remoteAddress) && callLog.localAddress.weakEqual(
                    log.localAddress
                )
            ) {
                Log.i(
                    "[xxxHistory Detail] New call log for ${callLog.remoteAddress.asStringUriOnly()} with local address ${callLog.localAddress.asStringUriOnly()}"
                )
                addRelatedCallLogs(arrayListOf(log))
            }
        }
    }

    val isConferenceCallLog = callLog.wasConference()

    val conferenceSubject = callLog.conferenceInfo?.subject
    val conferenceParticipantsData = MutableLiveData<ArrayList<ConferenceSchedulingParticipantData>>()
    val organizerParticipantData = MutableLiveData<ConferenceSchedulingParticipantData>()
    val conferenceTime = MutableLiveData<String>()
    val conferenceDate = MutableLiveData<String>()

    val readOnlyNativeAddressBook = MutableLiveData<Boolean>()

    override val showGroupChatAvatar: Boolean
        get() = isConferenceCallLog

    private val chatRoomListener = object : ChatRoomListenerStub() {
        override fun onStateChanged(chatRoom: ChatRoom, state: ChatRoom.State) {
            if (state == ChatRoom.State.Created) {
                waitForChatRoomCreation.postValue(false)
                chatRoomCreatedEvent.postValue(Event(chatRoom))
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("[xxxHistory Detail] Group chat room creation has failed !")
                waitForChatRoomCreation.postValue(false)
                onMessageToNotifyEvent.postValue(Event(R.string.chat_room_creation_failed_snack))
            }
        }
    }

    init {
        viewModelScope.launch {
            waitForChatRoomCreation.postValue(false)
            readOnlyNativeAddressBook.postValue(corePreferences.readOnlyNativeContacts)

            if (!isRelated) {
                val conferenceInfo = callLog.conferenceInfo
                if (conferenceInfo != null) {
                    conferenceTime.postValue(TimestampUtils.timeToString(conferenceInfo.dateTime))
                    conferenceDate.postValue(
                        if (TimestampUtils.isToday(conferenceInfo.dateTime)) {
                            AppUtils.getString(R.string.today)
                        } else {
                            TimestampUtils.toString(
                                conferenceInfo.dateTime,
                                onlyDate = true,
                                shortDate = false,
                                hideYear = false
                            )
                        }
                    )
                    val organizer = conferenceInfo.organizer
                    if (organizer != null) {
                        organizerParticipantData.postValue(
                            ConferenceSchedulingParticipantData(
                                organizer,
                                showLimeBadge = false,
                                showDivider = false
                            )
                        )
                    }
                    val list = arrayListOf<ConferenceSchedulingParticipantData>()
                    for (participant in conferenceInfo.participants) {
                        list.add(
                            ConferenceSchedulingParticipantData(
                                participant,
                                showLimeBadge = false,
                                showDivider = true
                            )
                        )
                    }
                    conferenceParticipantsData.postValue(list)
                }
            }
        }
    }

    override fun onCleared() {
        destroy()
        super.onCleared()
    }

    fun destroy() {
        if (!isRelated) {
            relatedCallLogs.value.orEmpty().forEach(CallLogViewModel::destroy)
            organizerParticipantData.value?.destroy()
            conferenceParticipantsData.value.orEmpty()
                .forEach(ConferenceSchedulingParticipantData::destroy)
        }
    }

    fun startCall() {
        Log.i("[xxxHistory Detail] startCall")
        startCallEvent.postValue(Event(callLog))
    }

    fun startVideoCall() {
        Log.i("[xxxHistory Detail] startVideoCall")
        startVideoCallEvent.postValue(Event(callLog))
    }

    fun startChat(isSecured: Boolean) {
        waitForChatRoomCreation.postValue(true)
        val chatRoom = LinphoneUtils.createOneToOneChatRoom(callLog.remoteAddress, isSecured)

        if (chatRoom != null) {
            val state = chatRoom.state
            Log.i("[History Detail] Found existing chat room in state $state")
            if (state == ChatRoom.State.Created || state == ChatRoom.State.Terminated) {
                waitForChatRoomCreation.postValue(false)
                chatRoomCreatedEvent.postValue(Event(chatRoom))
            } else {
                chatRoom.addListener(chatRoomListener)
            }
        } else {
            waitForChatRoomCreation.postValue(false)
            Log.e(
                "[History Detail] Couldn't create chat room with address ${callLog.remoteAddress}"
            )
            onMessageToNotifyEvent.postValue(Event(R.string.chat_room_creation_failed_snack))
        }
    }

    fun addRelatedCallLogs(callLogs: ArrayList<CallLog>) {
        val list = arrayListOf<CallLogViewModel>()

        // We assume new logs are more recent than the ones we already have, so we add them first
        for (callLog in callLogs) {
            list.add(CallLogViewModel(callLog, true))
        }
        list.addAll(relatedCallLogs.value.orEmpty())

        relatedCallLogs.postValue(list)
    }

    fun enableListener(enable: Boolean) {
        if (enable) {
            coreContext.core.addListener(listener)
        } else {
            coreContext.core.removeListener(listener)
        }
    }
}
