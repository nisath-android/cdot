/*
 * Copyright (c) 2010-2021 Belledonne Communications SARL.
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
package org.naminfo.contact

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.core.Address
import org.linphone.core.ChatRoom.SecurityLevel
import org.linphone.core.ConsolidatedPresence
import org.linphone.core.Friend
import org.naminfo.LinphoneApplication
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.activities.main.contact.viewmodels.MockContactList
import org.naminfo.activities.main.viewmodels.MessageNotifierViewModel

interface ContactDataInterface {

    val contact: MutableLiveData<Friend>

    val displayName: MutableLiveData<String>

    val securityLevel: MutableLiveData<SecurityLevel>

    val showGroupChatAvatar: Boolean
        get() = false

    val presenceStatus: MutableLiveData<ConsolidatedPresence>

    val coroutineScope: CoroutineScope
}

private const val TAG = "==>>ContactDataInterface"

open class GenericContactData(private val sipAddress: Address) : ContactDataInterface {
    final override val contact: MutableLiveData<Friend> = MutableLiveData<Friend>()
    final override val displayName: MutableLiveData<String> = MutableLiveData<String>()
    final override val securityLevel: MutableLiveData<SecurityLevel> =
        MutableLiveData<SecurityLevel>()
    final override val presenceStatus: MutableLiveData<ConsolidatedPresence> =
        MutableLiveData<ConsolidatedPresence>()
    final override val coroutineScope: CoroutineScope = coreContext.coroutineScope

    init {
        securityLevel.value = SecurityLevel.ClearText
        presenceStatus.value = ConsolidatedPresence.Offline
        contactLookup()
    }

    open fun destroy() {
    }

    protected fun contactLookup() {
        // displayName.value = LinphoneUtils.getDisplayName(sipAddress)
        Log.d(
            "CallLogsAdapter",
            "1-contactLookup: GenericContactData=> " +
                "${sipAddress.methodParam} ," +
                "username=${sipAddress.username} ," +
                "displayName=${sipAddress.displayName}, " +
                "sip-uri=${sipAddress.asStringUriOnly()}" +
                "asStringUriOnly=${contact.value?.address?.asStringUriOnly()}" +
                "sip-uri-displayName=${contact.value?.address?.displayName}" +
                "sip-uri-username=${contact.value?.address?.username}"
        )

        val friend = coreContext.contactsManager.findContactByAddress(sipAddress)
        if (friend != null) {
            Log.i(
                "CallLogsAdapter",
                "1-sipAddress=$sipAddress,friend-name =${friend.name} friend-address=${friend.address},core=${friend.core.accountList}displayName =${displayName.value}"
            )
            contact.postValue(friend!!)
            presenceStatus.postValue(friend.consolidatedPresence)
            friend.addListener {
                presenceStatus.postValue(it.consolidatedPresence)
            }
        } else {
            Log.i(
                "CallLogsAdapter",
                "1-contactLookup =>Else username=${sipAddress?.username}"
            )
            /*if (sipAddress?.username != null && sipAddress?.username?.isNotEmpty() == true) {
                displayName.postValue(sipAddress?.username)
            }*/
        }
    }
}

abstract class GenericContactViewModel(private val sipAddressMain: Address) :
    MessageNotifierViewModel(), ContactDataInterface {
    final override val contact: MutableLiveData<Friend> = MutableLiveData<Friend>()
    final override val displayName: MutableLiveData<String> = MutableLiveData<String>()
    var contactNumber: MutableLiveData<String> = MutableLiveData<String>()
    final override val securityLevel: MutableLiveData<SecurityLevel> =
        MutableLiveData<SecurityLevel>()
    final override val presenceStatus: MutableLiveData<ConsolidatedPresence> =
        MutableLiveData<ConsolidatedPresence>()
    final override val coroutineScope: CoroutineScope = viewModelScope
    var sipAddress: Address? = null

    init {
        sipAddress = sipAddressMain
        viewModelScope.launch(Dispatchers.Main) {
            securityLevel.postValue(SecurityLevel.ClearText)
            presenceStatus.postValue(ConsolidatedPresence.Offline)
            contactLookup()
        }
    }

    private fun contactLookup() {
        Log.d(
            "CallLogsAdapter",
            "2-contactLookup: GenericContactViewModel=> username=${sipAddress?.username} " +
                ",displayName=${sipAddress?.displayName}, " +
                "sip-uri=${sipAddress?.asStringUriOnly()}" +
                "asStringUriOnly=${contact.value?.address?.asStringUriOnly()}" +
                "sip-uri-displayName=${contact.value?.address?.displayName}" +
                "sip-uri-username=${contact.value?.address?.username}" +
                "sip-uri-displayName=${displayName.value}"
        )
        /* Log.i(
             "CallLogsAdapter",
             " 2-contactLookup:sip-uri-contactNumber=${contactNumber.value} " + coreContext.core.callLogs.joinToString(
                 separator = "\n"
             ) { log ->
                 "CallLog from: ${log.fromAddress?.asStringUriOnly()} to: ${log.toAddress?.asStringUriOnly()} " +
                         "Duration: ${log.duration} seconds, Status: ${log.status}, Video: ${log.isVideoEnabled}\n"
             }
         )*/
        var friend: Friend? = null
        /* if (sipAddress?.username == "conference_video") {
             val conferenceNum = "5500"
             val addressString = MockContactList.SipValidator.replaceSipUsername(
                 sipAddress?.asStringUriOnly().toString(),
                 conferenceNum
             )

             val address = Factory.instance().createAddress(addressString)
             friend = coreContext.contactsManager.findContactByAddress(address!!)
            // friend = coreContext.contactsManager.findContactByPhoneNumber(conferenceNum)

         }
         else if (sipAddress?.username == "conference_audio") {
             val conferenceNum = "3500"
             val addressString = MockContactList.SipValidator.replaceSipUsername(
                 sipAddress?.asStringUriOnly().toString(),
                 conferenceNum
             )
             val address = Factory.instance().createAddress(addressString)
             friend = coreContext.contactsManager.findContactByAddress(address!!)
         }
         else {
             friend = coreContext.contactsManager.findContactByAddress(sipAddress!!)
         }*/
        friend = coreContext.contactsManager.findContactByAddress(sipAddress!!)
        if (friend != null) {
            Log.i(
                "CallLogsAdapter",
                "2-GenericContactViewModel=>friend-name =${friend.name} friend-address-username=${friend.address?.username},core.accountList=${friend.core.accountList.size}displayName =${displayName.value}"
            )
            try {
                // displayName.postValue(LinphoneUtils.getDisplayName(sipAddress!!))
                displayName.postValue(friend.name)
                contact.postValue(friend!!)
            } catch (e: Exception) {
                Log.e(
                    "CallLogsAdapter",
                    "Error ==> ${e.message}"
                )
            }
            presenceStatus.postValue(friend.consolidatedPresence)
            friend.addListener {
                presenceStatus.postValue(it.consolidatedPresence)
            }
        } else {
            val callerName = LinphoneApplication.corePreferences.getCallerName
            Log.i(
                "CallLogsAdapter",
                "2-GenericContactViewModel=>Else->username=${sipAddress?.username} ,selectedcontect=$callerName,size:${MockContactList.SipValidator.getCallHistory().size}"
            )
            /* if (sipAddress?.username != null && sipAddress?.username?.isNotEmpty() == true) {
                 displayName.postValue(sipAddress?.username)
             }*/
            if (sipAddress?.displayName?.isNotEmpty() == true) {
                if (sipAddress?.username.toString() == "0000000000") {
                    displayName.postValue(sipAddress?.displayName)
                }
            }
            // displayName.postValue(sipAddress?.asStringUriOnly())
            /*  viewModelScope.launch(Dispatchers.Main) {
                  val historyList = MockContactList.SipValidator.getCallHistory()
                  val contactsList = MockContactList.SipValidator.getSipContacts()
                  val contactsMap = contactsList.associateBy { it.phone }
                  historyList.forEach { history ->
                      val matched = contactsMap[history.phone]

                      if (matched != null) {
                          // Found matching contact
                          displayName.postValue(history.sipAddress)

                          Log.d(
                              "CallLogsAdapter",
                              "Matched: phone=${history.phone}, name=${history.name}, sip=${history.sipAddress}"
                          )
                      } else {
                          Log.d(
                              "CallLogsAdapter",
                              "Not Matched: phone=${history.phone}, name=${history.name}, sip=${history.sipAddress}"
                          )
                      }
                  }
              }*/
        }
    }
}
