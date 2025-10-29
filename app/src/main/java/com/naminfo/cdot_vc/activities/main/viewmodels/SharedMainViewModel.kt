package com.naminfo.cdot_vc.activities.main.viewmodels

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.history.GroupedCallLogData
import org.linphone.core.*
import com.naminfo.cdot_vc.utils.Event

class SharedMainViewModel : ViewModel() {
    //val selectedCallLogGroup: Any
    val toggleDrawerEvent = MutableLiveData<Event<Boolean>>()

    val layoutChangedEvent = MutableLiveData<Event<Boolean>>()
    var isSlidingPaneSlideable = MutableLiveData<Boolean>()

    /* Call history */

    val selectedCallLogGroup = MutableLiveData<GroupedCallLogData>()

    /* Chat */

    val selectedChatRoom = MutableLiveData<ChatRoom>()
    var destructionPendingChatRoom: ChatRoom? = null

    val selectedGroupChatRoom = MutableLiveData<ChatRoom>()

    val filesToShare = MutableLiveData<ArrayList<String>>()

    val textToShare = MutableLiveData<String>()

    /*val messageToForwardEvent: MutableLiveData<Event<ChatMessage>> by lazy {
        MutableLiveData<Event<ChatMessage>>()
    }*/

    val isPendingMessageForward = MutableLiveData<Boolean>()

    val contentToOpen = MutableLiveData<Content>()

    var createEncryptedChatRoom: Boolean = corePreferences.forceEndToEndEncryptedChat

    val chatRoomParticipants = MutableLiveData<ArrayList<Address>>()

    var chatRoomSubject: String = ""

    // When using keyboard to share gif or other, see RichContentReceiver & RichEditText classes
    /*val richContentUri = MutableLiveData<Event<Uri>>()

    val refreshChatRoomInListEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }*/

    /* Contacts */

    val selectedContact = MutableLiveData<Friend>()

    // For correct animations directions
    val updateContactsAnimationsBasedOnDestination: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    /* Accounts */

    val defaultAccountChanged: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val accountRemoved: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val publishPresenceToggled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val accountSettingsFragmentOpenedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    /* Call */

    var pendingCallTransfer: Boolean = false

    /* Conference */

    /*val addressOfConferenceInfoToEdit: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }*/

    /*val participantsListForNextScheduledMeeting: MutableLiveData<Event<ArrayList<Address>>> by lazy {
        MutableLiveData<Event<ArrayList<Address>>>()
    }*/

    /* Dialer */

    var dialerUri: String = ""

    // For correct animations directions
    val updateDialerAnimationsBasedOnDestination: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }
    fun getCurrentUserName() = corePreferences.getCurrentUserName
}
