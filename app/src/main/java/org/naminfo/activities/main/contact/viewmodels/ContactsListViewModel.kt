/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
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
package org.naminfo.activities.main.contact.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.Factory
import org.linphone.core.Ldap
import org.linphone.core.MagicSearch
import org.linphone.core.MagicSearchListenerStub
import org.linphone.core.SearchResult
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.main.contact.data.BroadcastContact
import org.naminfo.activities.main.contact.data.GroupSettingsContact
import org.naminfo.activities.main.contact.data.PBXContactsTable
import org.naminfo.activities.main.contact.data.PbxContact
import org.naminfo.contact.ContactsUpdatedListenerStub
import org.naminfo.utils.Event
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val TAG = "[Contact-ViewModel]"
class ContactsListViewModel : ViewModel() {
    val pbxContactsSelected = MutableLiveData<Boolean>().apply { value = false }
    private val _pbxContactsLiveData = MutableLiveData<List<PBXContactsTable>>()
    val pbxContactsLiveData: LiveData<List<PBXContactsTable>> get() = _pbxContactsLiveData
    private var allPbxContacts: List<PBXContactsTable> = emptyList()

    val bcContactsSelected = MutableLiveData<Boolean>().apply { value = false }
    private val _bcContactsLiveData = MutableLiveData<List<BroadcastContact>>()
    val bcContactsLiveData: LiveData<List<BroadcastContact>> get() = _bcContactsLiveData
    private var allBcContacts: List<BroadcastContact> = emptyList()

    val groupContactsSelected = MutableLiveData<Boolean>().apply { value = false }
    private val _groupSettingsContactsLiveData = MutableLiveData<List<GroupSettingsContact>>()
    val groupSettingsContactsLiveData: LiveData<List<GroupSettingsContact>> get() = _groupSettingsContactsLiveData
    private var allGroupSettingsContacts: List<GroupSettingsContact> = emptyList()

    val sipContactsSelected = MutableLiveData<Boolean>().apply { value = false }
    val isNetwork = MutableLiveData<Boolean>().apply { value = false }
    val isSipContactListEmpty = MutableLiveData<Boolean>().apply { value = false }

    // val sipContactsLiveData: LiveData<List<SipContact>> get() = _sipContactsLiveData
    val _sipContactsLiveData = MutableLiveData<List<SipContact>>()
    val sipContactsLiveData = MediatorLiveData<List<SipContact>>().apply {
        addSource(_sipContactsLiveData) { value = it ?: emptyList() }
    }
    var allSipContacts: List<SipContact> = emptyList()

    val contactsList = MutableLiveData<ArrayList<ContactViewModel>>()

    val nativeAddressBookEnabled = MutableLiveData<Boolean>()

    val readOnlyNativeAddressBook = MutableLiveData<Boolean>()

    val hideSipContactsList = MutableLiveData<Boolean>()

    val onlyShowSipContactsList = MutableLiveData<Boolean>()

    val fetchInProgress = MutableLiveData<Boolean>()
    private var searchResultsPending: Boolean = false
    private var fastFetchJob: Job? = null

    val filter = MutableLiveData<String>()
    private var previousFilter = "NotSet"

    val pbxSearchQuery = MutableLiveData<String>()
    val sipSearchQuery = MutableLiveData<String>()
    val groupSettingsSearchQuery = MutableLiveData<String>()
    val bcSearchQuery = MutableLiveData<String>()

    val domainMain = if (coreContext.core.authInfoList.isNotEmpty()) {
        coreContext.core.authInfoList[0].domain
    } else {
        null // Or handle this case appropriately
    }

    val moreResultsAvailableEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i(TAG, " onContactsUpdated=> before  updateContactsList clearcahe =true")
            updateContactsList(true)
        }
    }

    private val magicSearchListener = object : MagicSearchListenerStub() {
        override fun onSearchResultsReceived(magicSearch: MagicSearch) {
            Log.i(TAG, " onSearchResultsReceived=> 3-Magic search contacts available")
            viewModelScope.launch {
                searchResultsPending = false
                withContext(Dispatchers.Main) {
                    processMagicSearchResults(magicSearch.lastSearch)
                    // updateSipContactsList(clearCache = true)
                    // contactsList.value.orEmpty().forEach(ContactViewModel::destroy)
                    // Use coreContext.contactsManager.fetchInProgress instead of false in case contacts are still being loaded
                    fetchInProgress.value = coreContext.contactsManager.fetchInProgress.value
                }
            }
        }

        override fun onLdapHaveMoreResults(magicSearch: MagicSearch, ldap: Ldap) {
            Log.i(TAG, " onLdapHaveMoreResults=> 4-Magic search contacts available")
            moreResultsAvailableEvent.value = Event(true)
        }
    }

    init {
        Log.i(TAG, " init ")
        pbxContactsSelected.value = true
        sipContactsSelected.value = coreContext.contactsManager.shouldDisplaySipContactsList()
        nativeAddressBookEnabled.value = corePreferences.enableNativeAddressBookIntegration
        readOnlyNativeAddressBook.value = corePreferences.readOnlyNativeContacts

        onlyShowSipContactsList.value = corePreferences.onlyShowSipContactsList
        hideSipContactsList.value = corePreferences.hideSipContactsList
        if (onlyShowSipContactsList.value == true) {
            sipContactsSelected.value = true
        }
        if (hideSipContactsList.value == true) {
            sipContactsSelected.value = false
        }

        coreContext.contactsManager.addListener(contactsUpdatedListener)
        coreContext.contactsManager.magicSearch.addListener(magicSearchListener)
    }

    override fun onCleared() {
        Log.i(TAG, " onCleared ")
        contactsList.value.orEmpty().forEach(ContactViewModel::destroy)
        coreContext.contactsManager.magicSearch.removeListener(magicSearchListener)
        coreContext.contactsManager.removeListener(contactsUpdatedListener)

        super.onCleared()
    }

    fun updateContactsList(clearCache: Boolean) {
        val filterValue = filter.value.orEmpty()

        if (clearCache || (
            previousFilter.isNotEmpty() && (
                previousFilter.length > filterValue.length ||
                    (previousFilter.length == filterValue.length && previousFilter != filterValue)
                )
            )
        ) {
            coreContext.contactsManager.magicSearch.resetSearchCache()
        }
        previousFilter = filterValue

        val domain =
            if (sipContactsSelected.value == true) {
                coreContext.core.defaultAccount?.params?.domain
                    ?: ""
            } else {
                ""
            }
        val sources = MagicSearch.Source.Friends.toInt() or MagicSearch.Source.LdapServers.toInt()
        val aggregation = MagicSearch.Aggregation.Friend
        searchResultsPending = true
        fastFetchJob?.cancel()
        Log.i(
            TAG,
            " updateContactsList=> Asking Magic search for contacts matching filter [$filterValue], domain [$domain] and in sources [$sources]"
        )
        coreContext.contactsManager.magicSearch.getContactsListAsync(
            filterValue,
            domain,
            sources,
            aggregation
        )

        val spinnerDelay = corePreferences.delayBeforeShowingContactsSearchSpinner.toLong()
        fastFetchJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(spinnerDelay)
            }
            withContext(Dispatchers.Main) {
                if (searchResultsPending) {
                    fetchInProgress.value = true
                }
            }
        }
    }

    private fun processMagicSearchResults(results: Array<SearchResult>) {
        Log.i(TAG, " [Contacts] processMagicSearchResults=> Processing ${results.size} results")
        contactsList.value.orEmpty().forEach(ContactViewModel::destroy)
        // contactsList.value = arrayListOf()

        val list = arrayListOf<ContactViewModel>()
        //    val uniqueNumbers = mutableSetOf<ContactViewModel>() // Set to track unique mobile numbers

        /*for (result in results) {
            val friend = result.friend

            val viewModel = if (friend != null) {
                ContactViewModel(friend)
            } else {
                Log.w(TAG, " processMagicSearchResults=> SearchResult [$result] has no Friend!")
                val fakeFriend = coreContext.contactsManager.createFriendFromSearchResult(
                    result
                )
                ContactViewModel(fakeFriend)
            }

            list.add(viewModel)
            // Check for uniqueness based on `mobileNumber`
           */
        /* val mobileNumber = viewModel.contact.value?.phoneNumbers
            if (!mobileNumber.isNullOrEmpty()) {
                list.add(viewModel)
            } else {
                Log.w("[Contacts] Duplicate contact with mobile number [$mobileNumber] skipped")
            }*/
        /*
        }

        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                contactsList.value = list
                // _sipContactsLiveData.value = tempAllSipContacts
            }
        }*/
        // ✅ Add your mock contacts
        list.addAll(MockContactList.fetchSipMockContacts())
        viewModelScope.launch {
            contactsList.postValue(list)
        }

        Log.i(TAG, " processMagicSearchResults=> [Contacts] Processed ${results.size} results")
        Log.i(TAG, " processMagicSearchResults=>  Processed ${list.size} unique results")
    }

    // Function to update PBX contacts list based on the filter
    fun updatePbxContactsList(clearCache: Boolean) {
        // val allPbxContacts: List<PbxContact> = _pbxContactsLiveData.value ?: emptyList()
        val query = pbxSearchQuery.value.orEmpty()
        Log.i(TAG, "  updatePbxContactsList=>  PBX Query is $query")
        // Filter the contacts based on the search query
        val filteredContacts = if (query.isEmpty()) {
            allPbxContacts
        } else {
            allPbxContacts.filter { contact: PBXContactsTable ->
                // Check if the name contains the search query (case-insensitive)
                val lowercaseQuery = query.lowercase()
                contact.First_Name?.lowercase()?.startsWith(lowercaseQuery) == true
            }
        }

        _pbxContactsLiveData.value = filteredContacts
    }

    fun fetchPbxContacts(context: Context) {
        viewModelScope.launch {
            if (domainMain != null) {
                val contacts = withContext(Dispatchers.IO) {
                    if (MockContactList.isFakeDataEnabled) {
                        MockContactList.fetchPBXMockContacts()
                    } else {
                        fetchContacts(context, domainMain)
                    }
                }
                withContext(Dispatchers.Main) {
                    val contactList = contacts.sortedBy { it.First_Name?.uppercase() }
                    allPbxContacts = contactList
                    _pbxContactsLiveData.value = (contactList)
                }
            }
        }
    }

    fun fetchBcContacts(context: Context) {
        viewModelScope.launch {
            if (domainMain != null) {
                val contactsBc = withContext(Dispatchers.IO) {
                    if (corePreferences.getCurrentUserPhoneNumber == "1001" || corePreferences.getCurrentUserPhoneNumber == "9874563211") {
                        fetchBcContacts1(context, domainMain)
                    } else {
                        emptyList()
                    }
                }
                withContext(Dispatchers.Main) {
                    allBcContacts = contactsBc
                    _bcContactsLiveData.value = (contactsBc)
                }
            }
        }
    }

    private suspend fun fetchBcContacts1(context: Context, domainMain: String): List<BroadcastContact> {
        // Simulate delay like real network call
        viewModelScope.launch { delay(500) }

        // Mock data
        return listOf(
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
    }

    fun updateBcSettingsContactsList() {
        val query = bcSearchQuery.value.orEmpty()
        Log.i(TAG, "updateBcSettingsContactsList => Search query: $query")

        val filteredContacts = if (query.isEmpty()) {
            allBcContacts
        } else {
            val lowercaseQuery = query.lowercase()
            allBcContacts.filter { contact ->
                contact.bcName?.lowercase()?.startsWith(lowercaseQuery) == true
            }
        }

        _bcContactsLiveData.value = filteredContacts
    }

    // Function to update Group Settings contacts list based on the filter
    fun updateGroupSettingsContactsList() {
        val query =
            groupSettingsSearchQuery.value.orEmpty() // Assuming you have a MutableLiveData for search query
        Log.i(TAG, "  updateGroupSettingsContactsList=>  Group Settings Query is $query")

        // Filter the contacts based on the search query
        val filteredContacts = if (query.isEmpty()) {
            allGroupSettingsContacts
        } else {
            allGroupSettingsContacts.filter { contact: GroupSettingsContact ->
                // Check if the group name contains the search query (case-insensitive)
                val lowercaseQuery = query.lowercase()
                contact.groupName?.lowercase()?.contains(lowercaseQuery) == true
            }
        }

        // Prevent displaying empty contact when there are no matches
        _groupSettingsContactsLiveData.value = if (filteredContacts.isEmpty()) {
            emptyList()
        } else {
            filteredContacts
        }
    }

    fun fetchGroupSettingsContacts(context: Context) {
        viewModelScope.launch {
            if (domainMain != null) {
                // Switch to IO thread for background task
                val contacts = withContext(Dispatchers.IO) {
                    if (MockContactList.isFakeDataEnabled) {
                        MockContactList.fetchGroupSettingsMockContacts()
                    } else {
                        fetchGroupSettingsContacts(context, domainMain)
                    }
                }

                // Switch back to Main thread for UI update
                withContext(Dispatchers.Main) {
                    allGroupSettingsContacts = contacts
                    _groupSettingsContactsLiveData.value = contacts // Correct UI thread usage
                }
            }
        }
    }

    fun startCall(number: String) {
        Log.i(TAG, " startCall=> Audio Call")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            false // Disable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept =
            false // Disable video acceptance
        coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
        coreContext.core.isVideoDisplayEnabled = false
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number, isVideo = false)
    }

    fun startVideoCall(number: String) {
        Log.i(TAG, " startVideoCall=>  video Call")
        Log.i(TAG, " startVideoCall=> before-> video enable: ${coreContext.core.isVideoEnabled}")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            true // Enable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
        coreContext.core.isVideoCaptureEnabled = true // Enable video capture
        coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
        Log.i(TAG, " startVideoCall=>  after-> video enable :  ${coreContext.core.isVideoEnabled}")
        val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(addrPbx, isVideo = true)
    }

    fun startBcAudioCall(number: String) {
        Log.i(TAG, " startCall=> Audio Call")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            false // Disable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept =
            false // Disable video acceptance
        coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
        coreContext.core.isVideoDisplayEnabled = false
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number, isVideo = false)
    }

    fun startBcVideoCall(number: String) {
        Log.i(TAG, " startVideoCall=>  video Call")
        Log.i(TAG, " startVideoCall=> before-> video enable: ${coreContext.core.isVideoEnabled}")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            true // Enable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
        coreContext.core.isVideoCaptureEnabled = true // Enable video capture
        coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
        LinphoneApplication.coreContext.core.nativePreviewWindowId = null
        LinphoneApplication.coreContext.core.nativeVideoWindowId = null
        Log.i(TAG, " startVideoCall=>  after-> video enable :  ${coreContext.core.isVideoEnabled}")
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number, isVideo = true)
    }

    // Coroutine function to fetch contacts
    private suspend fun fetchContacts(context: Context, domainVal: String): List<PBXContactsTable> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "  fetchContacts=> ", "No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList() // Return empty list or handle it as you see fit
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }
            // val domain = "http://$domainVal/fs_webservice/WebService.asmx/Get_PbxExtention"
            val domain = "http://192.168.1.31/fs_webservice/WebService.asmx/GetFs_Subscriber_Profile"
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    // parseXml(parser)
                    parseXmlFromGetFs_Subscriber_Profile(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, " fetchContacts=> ", "Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e(TAG, "  fetchContacts=> ", "Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseXmlFromGetFs_Subscriber_Profile(parser: XmlPullParser): List<PBXContactsTable> {
        Log.i(TAG, "  parseXmlFromGetFs_Subscriber_Profile------>>>>--------->>>>>")
        val contacts = mutableListOf<PBXContactsTable>()
        var eventType = parser.eventType
        var currentContact: PBXContactsTable? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> currentContact = PBXContactsTable()
                        "First_Name" -> currentContact?.First_Name = parser.nextText()
                        "Last_Name" -> currentContact?.Last_Name = parser.nextText()
                        "Subscriber_ID" -> currentContact?.Subscriber_ID = parser.nextText()
                        "Country_Code" -> currentContact?.Country_Code = parser.nextText()
                        "Mobile_No" -> currentContact?.Mobile_No = parser.nextText()
                        "Subscriber_Password" ->
                            currentContact?.Subscriber_Password =
                                parser.nextText()

                        "City" -> currentContact?.City = parser.nextText()
                        "Country" -> currentContact?.Country = parser.nextText()
                        "SMSSent" -> currentContact?.SMSSent = parser.nextText().toBoolean()
                        "PBX" -> currentContact?.PBX = parser.nextText()
                        "status" -> currentContact?.status = parser.nextText().toIntOrNull()
                        "timezone" -> currentContact?.timezone = parser.nextText()
                        "Action_date" -> currentContact?.Action_date = parser.nextText()
                        "Id" -> currentContact?.Id = parser.nextText().toIntOrNull()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null && currentContact.PBX != null && currentContact.PBX.toString().isNotEmpty()) {
                        Log.i(
                            TAG,
                            " parsexml => END_TAG: currentContact.PBX= ${ currentContact?.PBX}-----"
                        )
                        contacts.add(currentContact)
                    }
                }
            }
            eventType = parser.next()
        }
        return contacts
    }

    // XML parsing function
    private fun parseXml(parser: XmlPullParser): List<PbxContact> {
        val contacts = mutableListOf<PbxContact>()
        var eventType = parser.eventType
        var currentContact: PbxContact? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> currentContact = PbxContact()
                        "Name" -> currentContact?.name = parser.nextText()
                        "Department" -> currentContact?.country = parser.nextText()
                        "Extension_Number" -> currentContact?.extensionNumber = parser.nextText()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
                        contacts.add(currentContact)
                    }
                }
            }
            eventType = parser.next()
        }
        return contacts
    }

    // Coroutine function to fetch group settings contacts
    private suspend fun fetchGroupSettingsContacts(
        context: Context,
        domainVal: String
    ): List<GroupSettingsContact> {
        return withContext(Dispatchers.IO) {
            val number = coreContext.core.authInfoList[0].username
            Log.e(TAG, " fetchGroupSettingsContacts=> Number - $number")
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, " fetchGroupSettingsContacts=> No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList() // Return empty list or handle as you see fit
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }
            val domain =
                "http://192.168.1.31/fs_webservice/WebService.asmx/GroupSetting_View?number=$number"
            Log.i(TAG, " fetchGroupSettingsContacts=> group contact-api = $domain")
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseGroupSettingsXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, " fetchGroupSettingsContacts=> ", "Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e(TAG, "  fetchGroupSettingsContacts=> ", "Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    // XML parsing function for group settings contacts
    private fun parseGroupSettingsXml(parser: XmlPullParser): List<GroupSettingsContact> {
        Log.i(TAG, " parseGroupSettingsXml=> parser start")
        val groupContacts = mutableListOf<GroupSettingsContact>()
        var eventType = parser.eventType
        var currentContact: GroupSettingsContact? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> currentContact = GroupSettingsContact()
                        "Groupnumber" -> currentContact?.groupNumber = parser.nextText()
                        "Action_Date" -> currentContact?.actionDate = parser.nextText()
                        "Id" -> currentContact?.id = parser.nextText()
                        "Group_Name" -> currentContact?.groupName = parser.nextText()
                        "conference" -> currentContact?.conference = parser.nextText()
                        "User_Details" -> currentContact?.userDetails = parser.nextText()
                        "calltype" -> currentContact?.callType = parser.nextText()
                        "Moderate" -> currentContact?.moderate = parser.nextText()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
                        if (currentContact.groupName?.isNotEmpty() == true) {
                            groupContacts.add(currentContact)
                            Log.e(
                                TAG,
                                " parseGroupSettingsXml=> Group Name - ${currentContact.groupName}"
                            )
                        } else {
                            Log.e(
                                TAG,
                                "  parseGroupSettingsXml=> Group Name - ${currentContact.groupName}"
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return groupContacts
    }

    // Network availability check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        return networkCapabilities != null && networkCapabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    // Define SIP contact data model
    data class SipContact(
        // var id: String? = null,
        var name: String? = null,
        var mobileNumber: String? = null
    )

    // Function to update PBX contacts list based on the filter
    fun updateSipContactsList(clearCache: Boolean) {
        // val allPbxContacts: List<PbxContact> = _pbxContactsLiveData.value ?: emptyList()
        //  val query = sipSearchQuery.value.orEmpty() //old
        val query = filter.value.orEmpty() // new
        Log.i(TAG, " updateSipContactsList=> PBX Query is $query")
        // Filter the contacts based on the search query
        val filteredContacts1 = if (query.isEmpty()) {
            allSipContacts
        } else {
            allSipContacts.filter { contact: SipContact ->
                // Check if the name contains the search query (case-insensitive)
                val lowercaseQuery = query.lowercase()
                contact.name?.lowercase()?.contains(lowercaseQuery) == true
            }
        }
        _sipContactsLiveData.value = filteredContacts1
    }

    // Function to fetch SIP contacts
    fun fetchSipContacts(context: Context) {
        viewModelScope.launch {
            if (domainMain != null) {
                try {
                    // Switch to background thread for heavy operation
                    val contacts = withContext(Dispatchers.IO) {
                        fetchSipContactList(context, domainMain)
                    }

                    // Update LiveData on Main thread
                    withContext(Dispatchers.Main) {
                        _sipContactsLiveData.value = contacts
                        allSipContacts = contacts
                        tempAllSipContacts = contacts
                    }

                    if (contacts.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            saveContacts(contacts, domainMain)
                        }
                        Log.i(TAG, " fetchSipContacts =>", "Contacts saved locally successfully")
                    } else {
                        Log.w(TAG, " fetchSipContacts =>", "No contacts to save")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, " fetchSipContacts =>", "Error fetching contacts: ${e.message}", e)
                }
            }
        }
    }

    // Coroutine function to fetch SIP contacts
    private suspend fun fetchSipContactList(context: Context, domainVal: String): List<SipContact> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "  fetchSipContactList=>", "No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList() // Return empty list if no internet connection
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }

            val domain = "http://192.168.1.31/fs_webservice/WebService.asmx/Get_MobionNumber"
            Log.e(TAG, "  fetchSipContactList=>", "api call ->> $domain")
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, " fetchSipContactList=>", " Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e(TAG, " fetchSipContactList=>", "Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    // XML parsing function for SIP contacts
    private fun parseSipXml(parser: XmlPullParser): List<SipContact> {
        val contacts = mutableListOf<SipContact>()
        var eventType = parser.eventType
        var currentContact: SipContact? = null
        // var currentId = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> {
                            currentContact = SipContact()
                            // currentId = parser.getAttributeValue(null, "diffgr:id") ?: ""
                        }

                        "Name" -> currentContact?.name = parser.nextText()
                        "Mobile_No" -> currentContact?.mobileNumber = parser.nextText()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
                        // Only add if name is not null
                        Log.e(
                            TAG,
                            " fetchSipContactList=>",
                            "END_TAG->Name: ${currentContact.name}"
                        )
                        if (!currentContact.name.isNullOrEmpty()) {
                            contacts.add(currentContact)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        // Sort contacts alphabetically by name before returning
        return contacts.sortedBy { it.name }
    }

    private suspend fun fetchSipContactList1(context: Context, domainVal: String): List<SipContact> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "fetchSipContactList=>", "No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList()
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }

            val urlString = "http://$domainVal/api/extensions_xml.php"
            Log.e(TAG, "fetchSipContactList=>", "api call ->> $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as javax.net.ssl.HttpsURLConnection // Use HttpsURLConnection because it’s https
            // connection.instanceFollowRedirects = true

            connection.sslSocketFactory = getCustomSSLSocketFactory(context)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            connection.inputStream.use { inputStream ->
                val responseText = inputStream.bufferedReader().use { it.readText() }
                Log.e("TAG", "Response: $responseText")
            }
            try {
                connection.inputStream.use { inputStream ->
                    val responseText = inputStream.bufferedReader().use { it.readText() }

                    // 👇 Print the raw response content
                    Log.e(TAG, "fetchSipContactList=> Raw Response:\n$responseText")

                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml1(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, "fetchSipContactList=>", "Host unreachable: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "fetchSipContactList=>", "Error: ${e.message}")
                emptyList()
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseSipXml1(parser: XmlPullParser): List<SipContact> {
        val contacts = mutableListOf<SipContact>()
        var eventType = parser.eventType
        var currentContact: SipContact? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "extension" -> {
                            currentContact = SipContact()
                        }
                        "outbond_name" -> currentContact?.name = parser.nextText()
                        "extension" -> currentContact?.mobileNumber = parser.nextText()
                        // You can also read extension_uuid or description if needed
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "extension" && currentContact != null) {
                        if (!currentContact.name.isNullOrEmpty()) {
                            contacts.add(currentContact)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return contacts.sortedBy { it.name }
    }

    fun saveContacts(contactList: List<SipContact>, domainVal: String) {
        Log.i(TAG, " saveContacts=>", "contactList: size=> ${contactList.size}")
        for (sipContact in contactList) {
            val friend = coreContext.core.createFriend()
            friend.name = sipContact.name.orEmpty()

            // Save mobile number as a phone number
            sipContact.mobileNumber?.let { number ->
                friend.addPhoneNumber(number)
                val addr = Factory.instance().createAddress("sip:$number@$domainVal")
                friend.addAddress(addr!!)
            }

            // Save friend to the default friend list
            coreContext.core.defaultFriendList?.addLocalFriend(friend)

            Log.i(TAG, " saveContacts=>", "Friends: ${friend.name}")
        }
    }

    fun refreshContactsList(context: Context) {
        if (sipContactsLiveData.value.isNullOrEmpty()) {
            Log.i(
                TAG,
                " refreshContactsList=>",
                "IF-tempAllSipContacts: $tempAllSipContacts | _sipContactsLiveData=${_sipContactsLiveData.value}"
            )
            viewModelScope.launch((Dispatchers.Main)) {
                _sipContactsLiveData.value = tempAllSipContacts
            }
        } else {
            Log.i(
                TAG,
                " refreshContactsList=>",
                "Else-tempAllSipContacts: $tempAllSipContacts | _sipContactsLiveData=${_sipContactsLiveData.value}"
            )
            sipContactsLiveData.value?.forEachIndexed { index, sipContact ->
                Log.i(
                    TAG,
                    " refreshContactsList=>",
                    "->Sip Contacts: ${sipContact.name}"
                )
                if (sipContact.mobileNumber == corePreferences.getCurrentUserPhoneNumber) {
                    corePreferences.getCurrentUserName = sipContact.name
                }
            }
        }
    }
    companion object SipContacts {
        var tempAllSipContacts: List<SipContact> = emptyList()
    }

    fun getCustomSSLSocketFactory(context: Context): SSLSocketFactory {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream = context.resources.openRawResource(R.raw.server_cert)
        val certificate = certificateFactory.generateCertificate(inputStream)
        inputStream.close()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("server", certificate)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)

        return sslContext.socketFactory
    }
}
