package com.naminfo.cdot_vc.activities.main.contact.viewmodels

import android.content.ContentProviderOperation
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.contact.data.ContactNumberOrAddressClickListener
import com.naminfo.cdot_vc.activities.main.contact.data.ContactNumberOrAddressData
import com.naminfo.cdot_vc.activities.main.viewmodels.MessageNotifierViewModel
import com.naminfo.cdot_vc.contact.ContactDataInterface
import com.naminfo.cdot_vc.contact.ContactsUpdatedListenerStub
import com.naminfo.cdot_vc.contact.hasLongTermPresence
import org.linphone.core.*
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.LinphoneUtils
import com.naminfo.cdot_vc.utils.PhoneNumberUtils
import com.naminfo.cdot_vc.BR


class ContactViewModelFactory(private val friend: Friend) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactViewModel(friend) as T
    }
}

class ContactViewModel(friend: Friend) : MessageNotifierViewModel(), ContactDataInterface {
    override val contact: MutableLiveData<Friend> = MutableLiveData<Friend>()
    override val displayName: MutableLiveData<String> = MutableLiveData<String>()
    override val securityLevel: MutableLiveData<ChatRoom.SecurityLevel> = MutableLiveData<ChatRoom.SecurityLevel>()
    override val presenceStatus: MutableLiveData<ConsolidatedPresence> = MutableLiveData<ConsolidatedPresence>()
    override val coroutineScope: CoroutineScope = viewModelScope

    var fullName = ""

    val displayOrganization = corePreferences.displayOrganization

    val numbersAndAddresses = MutableLiveData<ArrayList<ContactNumberOrAddressData>>()

    val sendSmsToEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    val startCallToEvent: MutableLiveData<Event<Address>> by lazy {
        MutableLiveData<Event<Address>>()
    }

    val startVideoCallToEvent: MutableLiveData<Event<Address>> by lazy {
        MutableLiveData<Event<Address>>()
    }

    val chatRoomCreatedEvent: MutableLiveData<Event<ChatRoom>> by lazy {
        MutableLiveData<Event<ChatRoom>>()
    }

    val waitForChatRoomCreation = MutableLiveData<Boolean>()

    val isNativeContact = MutableLiveData<Boolean>()

    val readOnlyNativeAddressBook = MutableLiveData<Boolean>()

    val hasLongTermPresence = MutableLiveData<Boolean>()

    private val chatRoomListener = object : ChatRoomListenerStub() {
        override fun onStateChanged(chatRoom: ChatRoom, state: ChatRoom.State) {
            if (state == ChatRoom.State.Created) {
                chatRoom.removeListener(this)
                waitForChatRoomCreation.value = false
                chatRoomCreatedEvent.value = Event(chatRoom)
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("[Contact Detail] Group chat room creation has failed !")
                chatRoom.removeListener(this)
                waitForChatRoomCreation.value = false
                onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            }
        }
    }

    private val contactsListener = object : ContactsUpdatedListenerStub() {
        override fun onContactUpdated(friend: Friend) {
            if (friend.refKey == contact.value?.refKey) {
                Log.i("[Contact Detail] Friend has been updated!")
                contact.value = friend
                displayName.value = friend.name
                isNativeContact.value = friend.refKey != null
                updateNumbersAndAddresses()
            }
        }
    }

    private val listener = object : ContactNumberOrAddressClickListener {
        override fun onCall(address: Address) {
            startCallToEvent.value = Event(address)
        }

        override fun onVideoCall(address: Address) {
            startVideoCallToEvent.value = Event(address)
        }

        override fun onChat(address: Address, isSecured: Boolean) {
            waitForChatRoomCreation.value = true
            val chatRoom = LinphoneUtils.createOneToOneChatRoom(address, isSecured)

            if (chatRoom != null) {
                val state = chatRoom.state
                Log.i("[Contact Detail] Found existing chat room in state $state")
                if (state == ChatRoom.State.Created || state == ChatRoom.State.Terminated) {
                    waitForChatRoomCreation.value = false
                    chatRoomCreatedEvent.value = Event(chatRoom)
                } else {
                    chatRoom.addListener(chatRoomListener)
                }
            } else {
                waitForChatRoomCreation.value = false
                Log.e("[Contact Detail] Couldn't create chat room with address $address")
                onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            }
        }

        override fun onSmsInvite(number: String) {
            sendSmsToEvent.value = Event(number)
        }
    }

    init {
        viewModelScope.launch {
            try {

                withContext(Dispatchers.Main) {
                    try {
                        fullName = friend?.getName() ?: ""
                    } catch (e: Exception) {
                        Log.i("#####", "error=${e.message}")
                    }
                }
                contact.value = friend
                displayName.value = friend.name
                isNativeContact.value = friend.refKey != null
                presenceStatus.value = friend.consolidatedPresence
                readOnlyNativeAddressBook.value = corePreferences.readOnlyNativeContacts
                hasLongTermPresence.value = friend.hasLongTermPresence()

                friend.addListener {
                    presenceStatus.value = it.consolidatedPresence
                    hasLongTermPresence.value = it.hasLongTermPresence()
                }
            } catch (e: Exception) {
                Log.i("#####", "error=${e.message}")
            }
        }
    }

    override fun onCleared() {
        destroy()
        super.onCleared()
    }

    fun destroy() {
    }

    fun registerContactListener() {
        coreContext.contactsManager.addListener(contactsListener)
    }

    fun unregisterContactListener() {
        coreContext.contactsManager.removeListener(contactsListener)
    }

    fun deleteContact() {
        val select = ContactsContract.Data.CONTACT_ID + " = ?"
        val ops = java.util.ArrayList<ContentProviderOperation>()

        val id = contact.value?.refKey
        if (id != null) {
            Log.i("[Contact] Setting Android contact id $id to batch removal")
            val args = arrayOf(id)
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(select, args)
                    .build()
            )
        }

        contact.value?.remove()

        if (ops.isNotEmpty()) {
            try {
                Log.i("[Contact] Removing ${ops.size} contacts")
                coreContext.context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                Log.e("[Contact] $e")
            }
        }
    }

    fun updateNumbersAndAddresses() {
        val list = arrayListOf<ContactNumberOrAddressData>()
        val friend = contact.value ?: return

        for (address in friend.addresses) {
            val username = address.username
            if (username in friend.phoneNumbers) continue

            val value = address.asStringUriOnly()
            val presenceModel = friend.getPresenceModelForUriOrTel(value)
            val hasPresence = presenceModel?.basicStatus == PresenceBasicStatus.Open
            val isMe = coreContext.core.defaultAccount?.params?.identityAddress?.weakEqual(address) ?: false
            val hasLimeCapability = corePreferences.allowEndToEndEncryptedChatWithoutPresence || (
                friend.getPresenceModelForUriOrTel(
                    value
                )?.hasCapability(Friend.Capability.LimeX3Dh) ?: false
                )
            val secureChatAllowed = LinphoneUtils.isEndToEndEncryptedChatAvailable() && !isMe && hasLimeCapability
            val displayValue = if (coreContext.core.defaultAccount?.params?.domain == address.domain) (address.username ?: value) else value
            val noa = ContactNumberOrAddressData(
                address,
                hasPresence,
                displayValue,
                showSecureChat = secureChatAllowed,
                listener = listener
            )
            list.add(noa)
        }

        for (phoneNumber in friend.phoneNumbersWithLabel) {
            val number = phoneNumber.phoneNumber
            val presenceModel = friend.getPresenceModelForUriOrTel(number)
            val hasPresence = presenceModel != null && presenceModel.basicStatus == PresenceBasicStatus.Open
            val contactAddress = presenceModel?.contact ?: number
            val address = coreContext.core.interpretUrl(
                contactAddress,
                LinphoneUtils.applyInternationalPrefix()
            )
            address?.displayName = displayName.value.orEmpty()
            val isMe = if (address != null) {
                coreContext.core.defaultAccount?.params?.identityAddress?.weakEqual(
                    address
                ) ?: false
            } else {
                false
            }
            val hasLimeCapability = corePreferences.allowEndToEndEncryptedChatWithoutPresence || (
                friend.getPresenceModelForUriOrTel(
                    number
                )?.hasCapability(Friend.Capability.LimeX3Dh) ?: false
                )
            val secureChatAllowed = LinphoneUtils.isEndToEndEncryptedChatAvailable() && !isMe && hasLimeCapability
            val label = PhoneNumberUtils.vcardParamStringToAddressBookLabel(
                coreContext.context.resources,
                phoneNumber.label ?: ""
            )
            val noa = ContactNumberOrAddressData(
                address,
                hasPresence,
                number,
                isSip = false,
                showSecureChat = secureChatAllowed,
                typeLabel = label,
                listener = listener
            )
            list.add(noa)
        }
        numbersAndAddresses.postValue(list)
    }
}
