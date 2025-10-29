package com.naminfo.cdot_vc.contact

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.activities.main.viewmodels.MessageNotifierViewModel
import org.linphone.core.Address
import org.linphone.core.ChatRoom.SecurityLevel
import org.linphone.core.ConsolidatedPresence
import org.linphone.core.Friend
import com.naminfo.cdot_vc.utils.LinphoneUtils

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
    final override val securityLevel: MutableLiveData<SecurityLevel> = MutableLiveData<SecurityLevel>()
    final override val presenceStatus: MutableLiveData<ConsolidatedPresence> = MutableLiveData<ConsolidatedPresence>()
    final override val coroutineScope: CoroutineScope = coreContext.coroutineScope

    init {
        securityLevel.value = SecurityLevel.ClearText
        presenceStatus.value = ConsolidatedPresence.Offline
        contactLookup()
    }

    open fun destroy() {
    }

    protected fun contactLookup() {
        displayName.value = LinphoneUtils.getDisplayName(sipAddress)
        Log.d(
            TAG,
            "contactLookup: GenericContactData=> username=${sipAddress.username} ,displayName=${sipAddress.displayName}, sip-uri=${sipAddress.asStringUriOnly()}"
        )
        val friend = coreContext.contactsManager.findContactByAddress(sipAddress)
        if (friend != null) {
            Log.i(
                TAG,
                "sipAddress=$sipAddress,friend-name =${friend.name} friend-address=${friend.address},core=${friend.core.accountList}displayName =${displayName.value}"
            )
            contact.value = friend!!
            presenceStatus.value = friend.consolidatedPresence
            friend.addListener {
                presenceStatus.value = it.consolidatedPresence
            }
        }
    }
}

abstract class GenericContactViewModel(private val sipAddress: Address) : MessageNotifierViewModel(), ContactDataInterface {
    final override val contact: MutableLiveData<Friend> = MutableLiveData<Friend>()
    final override val displayName: MutableLiveData<String> = MutableLiveData<String>()
    var contactNumber: MutableLiveData<String> = MutableLiveData<String>()
    final override val securityLevel: MutableLiveData<SecurityLevel> = MutableLiveData<SecurityLevel>()
    final override val presenceStatus: MutableLiveData<ConsolidatedPresence> = MutableLiveData<ConsolidatedPresence>()
    final override val coroutineScope: CoroutineScope = viewModelScope

    init {
        securityLevel.value = SecurityLevel.ClearText
        presenceStatus.value = ConsolidatedPresence.Offline
        contactLookup()
    }

    private fun contactLookup() {
        Log.d(
            TAG,
            "2-contactLookup: GenericContactViewModel=> username=${sipAddress.username} ,displayName=${sipAddress.displayName}, sip-uri=${sipAddress.asStringUriOnly()}"
        )
        displayName.value = LinphoneUtils.getDisplayName(sipAddress)
        val friend = coreContext.contactsManager.findContactByAddress(sipAddress)
        if (friend != null) {
            Log.i(
                TAG,
                "1-GenericContactViewModel=>friend-name =${friend.name} friend-address-username=${friend.address?.username},core.accountList=${friend.core.accountList.size}displayName =${displayName.value}"
            )
            contact.value = friend!!
            presenceStatus.value = friend.consolidatedPresence
            friend.addListener {
                presenceStatus.value = it.consolidatedPresence
            }
        }
    }
}
