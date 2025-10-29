package com.naminfo.cdot_vc.activities.main.contact.data

import androidx.annotation.WorkerThread
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.ContactViewModel
import org.linphone.core.Factory
import org.linphone.core.Friend
import org.linphone.core.FriendList
import org.linphone.core.PresenceBasicStatus
import org.linphone.core.SubscribePolicy
import org.linphone.core.tools.Log

object MockContactList {
    val isFakeDataEnabled: Boolean = true

    @WorkerThread
    private fun createFriendFromSipContact(phoneNumber: String, userName: String): Friend {
        val core = coreContext.core
        val domain = corePreferences.defaultDomain

        val addressString = "sip:$phoneNumber@$domain"
        val address = Factory.instance().createAddress(addressString)

        val friend = core.createFriend().apply {
            edit()
            // this.name = phoneNumber
            this.setName(userName)
            this.addPhoneNumber(phoneNumber)
            address?.let { addAddress(it) }
            refKey = addressString
            isSubscribesEnabled = true

            starred = false

            done()
        }
        friend.presenceModel?.basicStatus = PresenceBasicStatus.Open
        friend.incSubscribePolicy = SubscribePolicy.SPAccept
        Log.i("[*****] addCustomSipContact-> publishPresence=${corePreferences.publishPresence}")
        corePreferences.publishPresence = true
        var friendList = core.getFriendListByName(userName)
        if (friendList == null) {
            friendList = core.createFriendList().apply {
                displayName = userName
                isDatabaseStorageEnabled = false
                isSubscriptionsEnabled = true
                type = FriendList.Type.Default
            }
            core.addFriendList(friendList)
        }
        friendList.addFriend(friend)
        return friend
    }
    private fun createMockSipContacts(phone: String, name: String): ContactViewModel {
        val fakeFriend = createFriendFromSipContact(phone, name)
        return ContactViewModel(fakeFriend)
    }
    fun fetchSipMockContacts(): List<ContactViewModel> {
        return listOf(
            createMockSipContacts(
                "9874563211",
                "Cdot-1"
            ),
            createMockSipContacts(
                "9874563212",
                "Cdot-2"
            ),
            createMockSipContacts(
                "9874563213",
                "Cdot-3"
            ),
            createMockSipContacts(
                "9874563214",
                "Cdot-4"
            ),
            createMockSipContacts(
                "9874563215",
                "Cdot-5"
            ),
            createMockSipContacts(
                "9874563216",
                "Cdot-6"
            ),
            createMockSipContacts(
                "9874563217",
                "Cdot-7"
            ),
            createMockSipContacts(
                "9874563218",
                "Cdot-8"
            ),
            createMockSipContacts(
                "9874563219",
                "Cdot-9"
            )

        )
    }

    fun fetchGroupSettingsMockContacts(): List<GroupSettingsContact> {
        return listOf(
            GroupSettingsContact(
                groupName = "Video Conference",
                groupNumber = "3500",
                conference = "Video",
                actionDate = "2025-03-31T11:52:13.28+05:30",
                userDetails = "Madhan Anandan-7339260551,nishanth pic-7845470737",
                callType = "Dial-IN"
            ),
            GroupSettingsContact(
                groupName = "Audio Conference",
                groupNumber = "5500",
                conference = "Audio",
                actionDate = "2025-03-31T11:52:13.28+05:30",
                userDetails = "Madhan Anandan-7339260551,nishanth pic-7845470737",
                callType = "Dial-IN"
            )
            /*GroupSettingsContact(
                groupName = "GroupName1",
                groupNumber = "3600",
                conference = "Audio",
                actionDate = "2025-03-31T11:52:13.28+05:30",
                userDetails = "Madhan Anandan-7339260551,nishanth pic-7845470737",
                callType = "Dial-OUT"
            )*/

        )
    }
}
