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

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.utils.AppUtils
import org.naminfo.utils.Event
import org.naminfo.utils.LinphoneUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ChatRoomCreationViewModel() : ViewModel() {
    val chatRoomCreatedEvent: MutableLiveData<Event<ChatRoom>> by lazy {
        MutableLiveData<Event<ChatRoom>>()
    }

    val domain = if (coreContext.core.authInfoList.isNotEmpty()) {
        coreContext.core.authInfoList[0].domain
    } else {
        null // Or handle this case appropriately
    }

    // Private mutable LiveData to store SIP contacts
    private val _sipContactsLiveData = MutableLiveData<List<SipContactViewModel.SipContact>>()

    // Public LiveData to observe from UI
    val sipContactsLiveData: LiveData<List<SipContactViewModel.SipContact>> get() = _sipContactsLiveData

    private var allSipContacts: List<SipContactViewModel.SipContact> = emptyList()

    val sipContactsSelected = MutableLiveData<Boolean>()

    val search = MutableLiveData<String>()

    val createGroupChat = MutableLiveData<Boolean>()

    val isEncrypted = MutableLiveData<Boolean>()

    val waitForChatRoomCreation = MutableLiveData<Boolean>()

    val secureChatAvailable = MutableLiveData<Boolean>()

    val secureChatMandatory: Boolean = corePreferences.forceEndToEndEncryptedChat

    private val listener = object : ChatRoomListenerStub() {
        override fun onStateChanged(room: ChatRoom, state: ChatRoom.State) {
            if (state == ChatRoom.State.Created) {
                waitForChatRoomCreation.value = false
                Log.i("[Chat Room Creation] Chat room created")
                chatRoomCreatedEvent.value = Event(room)
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("[Chat Room Creation] Group chat room creation has failed !")
                waitForChatRoomCreation.value = false
                // onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            }
        }
    }

    init {
        createGroupChat.value = false
        isEncrypted.value = secureChatMandatory
        waitForChatRoomCreation.value = false
        secureChatAvailable.value = LinphoneUtils.isEndToEndEncryptedChatAvailable()
    }

    fun updateEncryption(encrypted: Boolean) {
        if (!encrypted && secureChatMandatory) {
            Log.w(
                "[Chat Room Creation] Something tries to force plain text chat room even if secureChatMandatory is enabled!"
            )
            return
        }
        isEncrypted.value = encrypted
    }

    fun createOneToOneChat(searchResult: SipContactViewModel.SipContact) {
        waitForChatRoomCreation.value = true
        val defaultAccount = coreContext.core.defaultAccount
        var room: ChatRoom?
        var successResult = false
        val address = coreContext.core.interpretUrl(
            searchResult.mobileNumber ?: "",
            LinphoneUtils.applyInternationalPrefix()
        )
        if (address == null) {
            Log.i(
                "[Chat Room Creation] Can't get a valid address from search result $address"
            )
            // onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            waitForChatRoomCreation.value = false
        }
        // Log.i(            "[Chat Room Creation] Address from search result ${username}"        )
        val encrypted = secureChatMandatory || isEncrypted.value == true
        val params: ChatRoomParams = coreContext.core.createDefaultChatRoomParams()
        params.backend = ChatRoom.Backend.Basic
        params.isGroupEnabled = false
        if (encrypted) {
            params.isEncryptionEnabled = true
            params.backend = ChatRoom.Backend.FlexisipChat
            params.ephemeralMode = if (corePreferences.useEphemeralPerDeviceMode) {
                ChatRoom.EphemeralMode.DeviceManaged
            } else {
                ChatRoom.EphemeralMode.AdminManaged
            }
            params.ephemeralLifetime = 0 // Make sure ephemeral is disabled by default
            Log.i(
                "[Chat Room Creation] Ephemeral mode is ${params.ephemeralMode}, lifetime is ${params.ephemeralLifetime}"
            )
            params.subject = AppUtils.getString(R.string.chat_room_dummy_subject)
        }

        val participants = arrayOf(address)
        val localAddress: Address? = defaultAccount?.params?.identityAddress

        val localAddr = "sip:${localAddress!!.username}@${localAddress.domain}"
        val localAddress1: Address? = Factory.instance().createAddress(localAddr)

        Log.i(
            "[Chat Room Creation] Local Address ${localAddress.username}, ${localAddress.domain},${localAddress.asStringUriOnly()} " +
                "Participants ${participants[0]!!.username}, ${participants[0]!!.domain}, ${participants[0]!!.asStringUriOnly()}," +
                "From Address from string: ${localAddress1!!.asStringUriOnly()}"
        )

        room = coreContext.core.searchChatRoom(params, localAddress, null, participants)
        if (room == null) {
            Log.w(
                "[Chat Room Creation] Couldn't find existing 1-1 chat room with remote ${address!!.asStringUriOnly()}, encryption=$encrypted and local identity ${localAddress.asStringUriOnly()}"
            )
            room = coreContext.core.createChatRoom(params, localAddress, participants)

            if (room != null) {
                Log.i(
                    "[Chat Room Creation] Chat room creation state ${room.state}"
                )
                if (encrypted) {
                    val state = room.state
                    if (state == ChatRoom.State.Created) {
                        Log.i("[Chat Room Creation] Found already created chat room, using it")
                        chatRoomCreatedEvent.value = Event(room)
                        waitForChatRoomCreation.value = false
                    } else {
                        Log.i(
                            "[Chat Room Creation] Chat room creation is pending [$state], waiting for Created state"
                        )
                        room.addListener(listener)
                    }
                } else {
                    chatRoomCreatedEvent.value = Event(room)
                    waitForChatRoomCreation.value = false
                    Log.i(
                        "[Chat Room Creation] Chat room creation state ${room.state}"
                    )
                    successResult = true
                }
            } else {
                Log.e(
                    "[Chat Room Creation] Couldn't create chat room with remote ${address.asStringUriOnly()} and local identity ${localAddress.asStringUriOnly()}"
                )
                waitForChatRoomCreation.value = false
                successResult = false
            }
        } else {
            Log.i(
                "[Chat Room Creation] Found existing 1-1 chat room with remote ${address!!.asStringUriOnly()}, encryption=$encrypted and local identity ${localAddress.asStringUriOnly()}"
            )
            chatRoomCreatedEvent.value = Event(room)
            waitForChatRoomCreation.value = false
            successResult = true
        }
        // return successResult
    }

    fun updateSipContactsList(clearCache: Boolean) {
        // val allPbxContacts: List<PbxContact> = _pbxContactsLiveData.value ?: emptyList()
        val query = search.value.orEmpty()
        Log.i("[Contacts] PBX Query is $query")
        // Filter the contacts based on the search query
        val filteredContacts1 = if (query.isEmpty()) {
            allSipContacts
        } else {
            allSipContacts.filter { contact: SipContactViewModel.SipContact ->
                // Check if the name contains the search query (case-insensitive)
                val lowercaseQuery = query.lowercase()
                contact.name?.lowercase()?.contains(lowercaseQuery) == true
            }
        }
        _sipContactsLiveData.value = filteredContacts1
    }

    // Fetch SIP contacts function in ViewModel
    fun fetchSipContacts(context: Context) {
        viewModelScope.launch {
            if (domain != null) {
                val contacts = fetchSipContactList(context, domain)
                allSipContacts = contacts
                _sipContactsLiveData.value = (contacts)
            }
        }
    }

    // Coroutine function to fetch SIP contacts
    private suspend fun fetchSipContactList(context: Context, domainVal: String): List<SipContactViewModel.SipContact> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.e("FetchSipContactsTask", "No internet connection")
                return@withContext emptyList() // Return empty list if no internet connection
            }

            val domain = "http://$domainVal/fs_webservice/WebService.asmx/Get_MobionNumber"
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e("FetchSipContactsTask", "Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e("FetchSipContactsTask", "Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    // XML parsing function for SIP contacts
    private fun parseSipXml(parser: XmlPullParser): List<SipContactViewModel.SipContact> {
        val contacts = mutableListOf<SipContactViewModel.SipContact>()
        var eventType = parser.eventType
        var currentContact: SipContactViewModel.SipContact? = null
        // var currentId = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> {
                            currentContact = SipContactViewModel.SipContact()
                            // currentId = parser.getAttributeValue(null, "diffgr:id") ?: ""
                        }

                        "Name" -> currentContact?.name = parser.nextText()
                        "Mobile_No" -> currentContact?.mobileNumber = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
                        // Only add if name is not null
                        if (!currentContact.name.isNullOrEmpty()) {
                            contacts.add(currentContact)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        // Sort contacts alphabetically by name before returning
        return contacts.sortedBy { it.name }
    }

    // Network availability check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        return networkCapabilities != null && networkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }
}
