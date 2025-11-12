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
package org.naminfo.activities.main.chat.viewmodels

import androidx.lifecycle.MutableLiveData
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.R
import org.naminfo.activities.main.chat.data.ChatRoomData
import org.naminfo.activities.main.viewmodels.MessageNotifierViewModel
import org.naminfo.compatibility.Compatibility
import org.naminfo.utils.Event
import org.naminfo.utils.LinphoneUtils

class ChatRoomsListViewModel : MessageNotifierViewModel() {
    val chatRooms = MutableLiveData<ArrayList<ChatRoomData>>()

    val fileSharingPending = MutableLiveData<Boolean>()

    val textSharingPending = MutableLiveData<Boolean>()

    val forwardPending = MutableLiveData<Boolean>()

    val groupChatAvailable = MutableLiveData<Boolean>()

    val chatRoomIndexUpdatedEvent: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    private val chatRoomListener = object : ChatRoomListenerStub() {
        override fun onStateChanged(chatRoom: ChatRoom, newState: ChatRoom.State) {
            if (newState == ChatRoom.State.Deleted) {
                Log.i(
                    "[Chat Rooms] Chat room [${LinphoneUtils.getChatRoomId(chatRoom)}] is in Deleted state, removing it from list"
                )
                val list = arrayListOf<ChatRoomData>()
                val id = LinphoneUtils.getChatRoomId(chatRoom)
                for (data in chatRooms.value.orEmpty()) {
                    if (data.id != id) {
                        list.add(data)
                    }
                }
                chatRooms.value = list
            }
        }
    }

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onChatRoomStateChanged(core: Core, chatRoom: ChatRoom, state: ChatRoom.State) {
            if (state == ChatRoom.State.Created) {
                Log.i(
                    "[Chat Rooms] Chat room [${LinphoneUtils.getChatRoomId(chatRoom)}] is in Created state, adding it to list text=${chatRoom.lastMessageInHistory?.utf8Text}"
                )

                if (!chatRoom.lastMessageInHistory?.utf8Text.isNullOrEmpty()) {
                    val data = ChatRoomData(chatRoom)

                    Log.i(
                        "[Chat Rooms]  ${LinphoneUtils.getDisplayName(core.currentCallRemoteAddress)}\n" +
                            "contact:${data.contact.value?.name}\n" +
                            "displayName:${data.displayName}"
                    )
                    val list = arrayListOf<ChatRoomData>()
                    list.add(data)
                    list.addAll(chatRooms.value.orEmpty())
                    chatRooms.value = list
                }
            } else if (state == ChatRoom.State.TerminationFailed) {
                Log.e(
                    "[Chat Rooms] Group chat room removal for address ${chatRoom.peerAddress.asStringUriOnly()} has failed !"
                )
                onMessageToNotifyEvent.value = Event(R.string.chat_room_removal_failed_snack)
            }
        }

        override fun onMessageSent(core: Core, chatRoom: ChatRoom, message: ChatMessage) {
            onChatRoomMessageEvent(chatRoom)
        }

        override fun onMessagesReceived(
            core: Core,
            chatRoom: ChatRoom,
            messages: Array<out ChatMessage>
        ) {
            onChatRoomMessageEvent(chatRoom)
        }

        override fun onChatRoomRead(core: Core, chatRoom: ChatRoom) {
            notifyChatRoomUpdate(chatRoom)
        }

        override fun onChatRoomEphemeralMessageDeleted(core: Core, chatRoom: ChatRoom) {
            notifyChatRoomUpdate(chatRoom)
        }

        override fun onChatRoomSubjectChanged(core: Core, chatRoom: ChatRoom) {
            notifyChatRoomUpdate(chatRoom)
        }
    }

    private var chatRoomsToDeleteCount = 0

    init {
        groupChatAvailable.value = LinphoneUtils.isGroupChatAvailable()
        updateChatRooms()
        coreContext.core.addListener(listener)
    }

    override fun onCleared() {
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    fun deleteChatRoom(chatRoom: ChatRoom?) {
        for (eventLog in chatRoom?.getHistoryMessageEvents(0).orEmpty()) {
            LinphoneUtils.deleteFilesAttachedToEventLog(eventLog)
        }

        chatRoomsToDeleteCount = 1
        if (chatRoom != null) {
            coreContext.notificationsManager.dismissChatNotification(chatRoom)
            Compatibility.removeChatRoomShortcut(coreContext.context, chatRoom)
            chatRoom.addListener(chatRoomListener)
            coreContext.core.deleteChatRoom(chatRoom)
        }
    }

    fun deleteChatRooms(chatRooms: ArrayList<ChatRoom>) {
        Log.i("[Chat Rooms] Deleting ${chatRooms.size} chat rooms")
        chatRoomsToDeleteCount = chatRooms.size
        for (chatRoom in chatRooms) {
            for (eventLog in chatRoom.getHistoryMessageEvents(0)) {
                LinphoneUtils.deleteFilesAttachedToEventLog(eventLog)
            }

            coreContext.notificationsManager.dismissChatNotification(chatRoom)
            Compatibility.removeChatRoomShortcut(coreContext.context, chatRoom)
            chatRoom.addListener(chatRoomListener)
            chatRoom.core.deleteChatRoom(chatRoom)
        }
    }

    fun updateChatRooms() {
        chatRooms.value.orEmpty().forEach(ChatRoomData::destroy)

        val list = arrayListOf<ChatRoomData>()
        for (chatRoom in coreContext.core.chatRooms) {
            list.add(ChatRoomData(chatRoom))
        }
        Log.i("[Chat Rooms] Updating chat rooms list size= ${list.size}")
        chatRooms.value = list
    }

    fun notifyChatRoomUpdate(chatRoom: ChatRoom) {
        val index = findChatRoomIndex(chatRoom)
        if (index == -1) {
            updateChatRooms()
        } else {
            chatRoomIndexUpdatedEvent.value = Event(index)
        }
    }

    private fun reorderChatRooms() {
        val list = arrayListOf<ChatRoomData>()
        list.addAll(chatRooms.value.orEmpty())
        list.sortByDescending { data -> data.chatRoom.lastUpdateTime }
        Log.i("[Chat Rooms] reorderChatRooms  list size= ${list.size}")
        chatRooms.value = list
    }

    private fun findChatRoomIndex(chatRoom: ChatRoom): Int {
        val id = LinphoneUtils.getChatRoomId(chatRoom)
        for ((index, data) in chatRooms.value.orEmpty().withIndex()) {
            if (id == data.id) {
                return index
            }
        }
        return -1
    }

    private fun onChatRoomMessageEvent(chatRoom: ChatRoom) {
        when (findChatRoomIndex(chatRoom)) {
            -1 -> {
                Log.i(
                    "[Chat Rooms] -1 onChatRoomMessageEvent  peerAddress = ${chatRoom.peerAddress.asStringUriOnly()}"
                )
                updateChatRooms()
            }

            0 -> {
                Log.i(
                    "[Chat Rooms] 0 onChatRoomMessageEvent  peerAddress = ${chatRoom.peerAddress.asStringUriOnly()}\n"
                )
                chatRoomIndexUpdatedEvent.value = Event(0)
            }

            else -> {
                Log.i(
                    "[Chat Rooms] else-> onChatRoomMessageEvent  peerAddress = ${chatRoom.peerAddress.asStringUriOnly()}"
                )
                reorderChatRooms()
            }
        }
    }

    fun createChatRoom() {
        val defaultAccount = coreContext.core.defaultAccount
        var room: ChatRoom?
        val address = "sip:9176066606@192.168.1.71"
        val peerAddress: Address = Factory.instance().createAddress(address)!!
        val params: ChatRoomParams = coreContext.core.createDefaultChatRoomParams()
        params.backend = ChatRoom.Backend.Basic
        params.isGroupEnabled = false
        params.isEncryptionEnabled = false

        val localAddress: Address = defaultAccount?.params?.identityAddress!!
        room = coreContext.core.createChatRoom(params, localAddress, peerAddress)
        val state = room?.state
        if (room != null) {
            Log.i("[Chat Room Creation] Chat room creation state $state")
        } else {
            Log.i("[Chat Room Creation] Chat room creation not done")
        }
    }
}
