package org.naminfo.activities.main.contact.viewmodels

import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import org.linphone.core.Factory
import org.linphone.core.Friend
import org.linphone.core.FriendList
import org.linphone.core.PresenceBasicStatus
import org.linphone.core.SubscribePolicy
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.activities.main.contact.data.BroadcastContact
import org.naminfo.activities.main.contact.data.GroupSettingsContact
import org.naminfo.activities.main.contact.data.PBXContactsTable

object MockContactList {
    val isFakeDataEnabled: Boolean = true

    @WorkerThread
    private fun createFriendFromSipContact(phoneNumber: String, userName: String): Friend {
        val core = coreContext.core
        val domain = corePreferences.defaultDomain
        /* val sipUri = "sip:$phoneNumber@$domain"
         val address = Factory.instance().createAddress(sipUri).apply {
             this!!.displayName = userName
         }

         return core.createFriend().apply {
             edit()
             this.address = address
             this.name = userName
             this.refKey = sipUri // Make sure it's unique
             this.isSubscribesEnabled = true
             done()
         }*/

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
        val sipContacts = listOf(
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
            ),
            createMockSipContacts(
                "1000",
                "Cdot-10"
            ),
            createMockSipContacts(
                "1001",
                "Cdot-11"
            ),
            createMockSipContacts(
                "1002",
                "Cdot-12"
            )

        )
  /*     val gson = Gson()
  val simpleList = sipContacts.map { contactVM ->
            val friend = contactVM.contact.value
            SimpleContact(
                phone = friend?.address?.username ?: "",
                name = friend?.name ?: "",
                sipAddress = friend?.address?.asStringUriOnly() ?: ""
            )
        }
        // corePreferences.sipContactsSaved = gson.toJson(simpleList)*/
        return sipContacts
    }

    fun fetchBroadcastContacts(): List<BroadcastContact> {
        val broadcastContact = listOf(
            BroadcastContact(
                bcNumber = "60000",
                bcName = "CDOT Test Broadcast1",
                userDetails = "Cdot-2-9874563212,Cdot-3-9874563213",
                moderator = "no"
            ),
            BroadcastContact(
                bcNumber = "60001",
                bcName = "CDOT Test Broadcast2",
                userDetails = "Cdot-4-9874563214,Cdot-5-9874563215",
                moderator = "no"
            )
        )
        return broadcastContact
    }
    fun fetchPBXMockContacts(): List<PBXContactsTable> {
        val gson = Gson()
        val pbxContacts = listOf(
            PBXContactsTable(
                First_Name = "PBXFirstName1",
                Last_Name = "PBXLastName1",
                Mobileuser = true,
                Mobile_No = "111", // usernumber we will use
                PBX = "111"
            ),
            PBXContactsTable(
                First_Name = "PBXFirstName2",
                Last_Name = "PBXLastName2",
                Mobileuser = true,
                Mobile_No = "222", // usernumber we will use
                PBX = "222"
            ),
            PBXContactsTable(
                First_Name = "PBXFirstName3",
                Last_Name = "PBXLastName3",
                Mobileuser = true,
                Mobile_No = "333", // usernumber we will use
                PBX = "333"
            )
        )
        corePreferences.pbxContactsSaved = gson.toJson(pbxContacts)
        return pbxContacts
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
    fun parseSipUri(sipUri: String): Pair<String?, String?> {
        // Clean up prefix if present
        val cleanUri = sipUri.removePrefix("sip:").removePrefix("sips:")

        // Split into user and domain
        val parts = cleanUri.split("@")

        return if (parts.size == 2) {
            val user = parts[0].trim()
            val domain = parts[1].trim()
            Pair(user, domain)
        } else {
            Pair(null, null) // invalid SIP format
        }
    }

    fun makeUrl(phone: String, domain: String): String {
        return "sip:$phone@$domain"
    }

    object SipValidator {
        private const val FIXED_DOMAIN = "([a-zA-Z0-9_.-]+)"

        // Allows phonenum OR text before @
        private val sipRegex = Regex(
            pattern = "^sips?:([a-zA-Z0-9_.-]+)@$FIXED_DOMAIN$"
        )
        private val phoneOnlyRegex = Regex(
            pattern = "^sip:([0-9]+)@$FIXED_DOMAIN$"
        )

        // Accept ONLY letters in username (text only)
        private val strictTextRegex = Regex(
            pattern = "^sip:([A-Za-z0-9_-]+)@$FIXED_DOMAIN$"
        )

        fun isValidTextSip(sipUri: String): Boolean {
            return strictTextRegex.matches(sipUri)
        }
        fun isValidPhoneSip(sipUri: String): Boolean {
            return phoneOnlyRegex.matches(sipUri)
        }
        fun isValidSip(sipUri: String): Boolean {
            return sipRegex.matches(sipUri)
        }

        fun replaceSipUsername(sipUri: String, newUsername: String): String {
            return sipUri.replace(Regex("^sip:[^@]+"), "sip:$newUsername")
        }
        private val gson by lazy { Gson() }
        private val sipListType: Type = object : TypeToken<ArrayList<SimpleContact>>() {}.type
        private val sipContactListType: Type = object : TypeToken<ArrayList<SimpleContact>>() {}.type

        fun getCallHistory(): ArrayList<SimpleContact> {
            return try {
                gson.fromJson(
                    LinphoneApplication.corePreferences.callHistorySaved ?: "[]",
                    sipListType
                ) ?: arrayListOf()
            } catch (e: Exception) {
                e.printStackTrace()
                arrayListOf()
            }
        }

        fun addCallHistory(phone: String, name: String, sipUrl: String) {
            val list = getCallHistory()

            // Add NEW entry on top
            list.add(
                0,
                SimpleContact(
                    phone = phone,
                    name = name,
                    sipAddress = sipUrl
                )
            )

            // Save updated list
            LinphoneApplication.corePreferences.callHistorySaved = gson.toJson(list)
        }

        fun clearHistory() {
            LinphoneApplication.corePreferences.callHistorySaved = "[]"
        }

        fun getSipContacts(): ArrayList<SimpleContact> {
            return try {
                gson.fromJson(
                    LinphoneApplication.corePreferences.sipContactsSaved ?: "[]",
                    sipContactListType
                ) ?: arrayListOf()
            } catch (e: Exception) {
                e.printStackTrace()
                arrayListOf()
            }
        }

        fun fetchBroadcastContacts(): ArrayList<BroadcastContact> {
            return ArrayList(MockContactList.fetchBroadcastContacts())
        }
        fun fetchGroupedContacts(): ArrayList<GroupSettingsContact> {
            return ArrayList(MockContactList.fetchGroupSettingsMockContacts())
        }
    }
}
data class SimpleContact(
    val phone: String,
    val name: String,
    val sipAddress: String
)
sealed class MatchedContact {
    data class Sip(val data: SimpleContact) : MatchedContact()
    data class Broadcast(val data: BroadcastContact) : MatchedContact()
    data class Group(val data: GroupSettingsContact) : MatchedContact()
}
