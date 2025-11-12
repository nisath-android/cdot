package org.naminfo.activities.main.contact.data

import org.linphone.core.Address
import org.naminfo.LinphoneApplication

class ContactNumberOrAddressData1(
    val address: Address?,
    val displayedValue: String,
    private val listener: ContactNumberOrAddress1ClickListener
) {
    val chatAllowed = !LinphoneApplication.corePreferences.disableChat

    val hidePlainChat = LinphoneApplication.corePreferences.forceEndToEndEncryptedChat

    fun startCall() {
        address ?: return
        listener.onCall(address)
    }

    fun startVideoCall() {
        address ?: return
        listener.onCall(address)
    }

    fun startChat(secured: Boolean) {
        address ?: return
        listener.onChat(address, secured)
    }

    fun smsInvite() {
        listener.onSmsInvite(displayedValue)
    }
}

interface ContactNumberOrAddress1ClickListener {
    fun onCall(address: Address)

    fun onVideoCall(address: Address)

    fun onChat(address: Address, isSecured: Boolean)

    fun onSmsInvite(number: String)
}
