package com.naminfo.cdot_vc.activities.main.contact.data

import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.ContactViewModel
import org.linphone.core.Address

class ContactNumberOrAddressData(
    var address: Address?,
    val hasPresence: Boolean,
    val displayedValue: String,
    val isSip: Boolean = true,
    val showSecureChat: Boolean = false,
    val typeLabel: String = "",
    private val listener: ContactNumberOrAddressClickListener
) {
    val showInvite = !hasPresence && !isSip && corePreferences.showContactInviteBySms

    val chatAllowed = !corePreferences.disableChat

    val hidePlainChat = corePreferences.forceEndToEndEncryptedChat

    fun startCall(contactNumberOrAddressData: ContactNumberOrAddressData) {
        // Try to get the address from contactNumberOrAddressData
        val resolvedAddress = contactNumberOrAddressData?.address ?: return

        // If address is a mutable property, assign it if needed
        address = address ?: resolvedAddress

        // Then trigger the listener
        if (address == null) return
        listener.onCall(address!!)
    }

    fun startVideoCall() {
        address ?: return
        if (address == null) return
        listener.onVideoCall(address!!)
    }

    fun startChat(secured: Boolean) {
        address ?: return
        if (address == null) return
        listener.onChat(address!!, secured)
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
