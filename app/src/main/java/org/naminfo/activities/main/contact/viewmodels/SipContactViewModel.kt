package org.naminfo.activities.main.contact.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.linphone.core.Address
import org.linphone.core.ChatRoom
import org.linphone.core.ChatRoomListenerStub
import org.linphone.core.Factory
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.activities.main.contact.data.ContactNumberOrAddress1ClickListener
import org.naminfo.activities.main.contact.data.ContactNumberOrAddressData1
import org.naminfo.contact.ContactsUpdatedListenerStub
import org.naminfo.utils.Event
import org.naminfo.utils.LinphoneUtils

class SipContactViewModelFactory(private val sipContact: ContactsListViewModel.SipContact) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SipContactViewModel(sipContact) as T
    }
}

class SipContactViewModel(sipContact: ContactsListViewModel.SipContact) : ViewModel() {
    var contact: MutableLiveData<ContactsListViewModel.SipContact> = MutableLiveData()
    val displayName: MutableLiveData<String> = MutableLiveData()
    val displayNumber: MutableLiveData<String> = MutableLiveData()

    var fullName = ""
    var domain = coreContext.core.authInfoList[0].domain
    var addr = ""
    val address: Address

    val numbersAndAddresses = MutableLiveData<ArrayList<ContactNumberOrAddressData1>>()

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

    val readOnlyNativeAddressBook = MutableLiveData<Boolean>()

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
                // onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            }
        }
    }

    private val contactsListener = object : ContactsUpdatedListenerStub() {
        override fun onContactUpdated(friend: Friend) {
           /* if (friend.refKey == contact.value?.refKey) {
                Log.i("[Contact Detail] Friend has been updated!")
                contact.value = friend
                displayName.value = friend.name
                isNativeContact.value = friend.refKey != null
                updateNumbersAndAddresses()
            }*/
        }
    }

    private val listener = object : ContactNumberOrAddress1ClickListener {
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
                // onMessageToNotifyEvent.value = Event(R.string.chat_room_creation_failed_snack)
            }
        }

        override fun onSmsInvite(number: String) {
            sendSmsToEvent.value = Event(number)
        }
    }

    fun startCall() {
        address ?: return
        listener.onCall(address)
    }

    fun startVideoCall() {
        address ?: return
        listener.onVideoCall(address)
    }

    fun startChat(secured: Boolean) {
        address ?: return
        listener.onChat(address, secured)
    }

    init {
        fullName = sipContact.name ?: ""
        contact.value = sipContact
        displayName.value = sipContact.name!!
        displayNumber.value = sipContact.mobileNumber!!
        val number = sipContact.mobileNumber
        addr = "sip:$number@$domain"
        address = Factory.instance().createAddress(addr)!!

        readOnlyNativeAddressBook.value = corePreferences.readOnlyNativeContacts
    }
}
