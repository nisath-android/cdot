package com.naminfo.cdot_vc.activities.main.contact.data

import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import org.linphone.core.Address

class ContactNumberOrAddressData(
    val address: Address?,
    val hasPresence: Boolean,
    val displayedValue: String,
    val isSip: Boolean = true,
    val showSecureChat: Boolean = false,
    val typeLabel: String = "",
    private val listener: ContactNumberOrAddressClickListener) {
    val showInvite = !hasPresence && !isSip && corePreferences.showContactInviteBySms

    val chatAllowed = !corePreferences.disableChat

    val hidePlainChat = corePreferences.forceEndToEndEncryptedChat

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

    fun smsInvite() {
        listener.onSmsInvite(displayedValue)
    }
}

interface ContactNumberOrAddressClickListener {
    fun onCall(address: Address)

    fun onVideoCall(address: Address)

    fun onChat(address: Address, isSecured: Boolean)

    fun onSmsInvite(number: String)
}
