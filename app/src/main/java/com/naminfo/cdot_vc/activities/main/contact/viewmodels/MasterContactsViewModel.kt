package com.naminfo.cdot_vc.activities.main.contact.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.contact.data.BroadcastContact
import com.naminfo.cdot_vc.activities.main.contact.data.GroupSettingsContact
import com.naminfo.cdot_vc.activities.main.contact.data.MockContactList
import com.naminfo.cdot_vc.contact.ContactsUpdatedListenerStub
import com.naminfo.cdot_vc.utils.Event
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
//import org.linphone.core.tools.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

class MasterContactsViewModel : ViewModel() {
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
    val hideSipContactsList = MutableLiveData<Boolean>()

    val onlyShowSipContactsList = MutableLiveData<Boolean>()


    val fetchInProgress = MutableLiveData<Boolean>()
    private var searchResultsPending: Boolean = false
    private var fastFetchJob: Job? = null

    val filter = MutableLiveData<String>()
    private var previousFilter = "NotSet"

    val groupSettingsSearchQuery = MutableLiveData<String>()
    val bcSearchQuery = MutableLiveData<String>()

    data class SipContact(
        // var id: String? = null,
        var name: String? = null,
        var mobileNumber: String? = null
    )

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
            Log.i("CDOT_VC"," onContactsUpdated=> before  updateContactsList clearcahe =true")
            updateContactsList(true)
        }
    }

    private val magicSearchListener = object : MagicSearchListenerStub() {
        override fun onSearchResultsReceived(magicSearch: MagicSearch) {
            Log.i("CDOT_VC"," onSearchResultsReceived=> 3-Magic search contacts available")
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
            Log.i("CDOT_VC"," onLdapHaveMoreResults=> 4-Magic search contacts available")
            moreResultsAvailableEvent.value = Event(true)
        }
    }

    init {
        Log.i("CDOT_VC", "[MasterContactsViewModel] init ")
        sipContactsSelected.value = coreContext.contactsManager.shouldDisplaySipContactsList()
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
        Log.i("CDOT_VC"," onCleared ")
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
        Log.i("CDOT_VC",

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
        Log.i("CDOT_VC", " [Contacts] processMagicSearchResults=> Processing ${results.size} results")
        contactsList.value.orEmpty().forEach(ContactViewModel::destroy)
        // contactsList.value = arrayListOf()

        val list = arrayListOf<ContactViewModel>()
        //    val uniqueNumbers = mutableSetOf<ContactViewModel>() // Set to track unique mobile numbers

        /*for (result in results) {
            val friend = result.friend

            val viewModel = if (friend != null) {
                ContactViewModel(friend)
            } else {
                Log.w( " processMagicSearchResults=> SearchResult [$result] has no Friend!")
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
        // Add your mock contacts
        list.addAll(MockContactList.fetchSipMockContacts())
        list.forEachIndexed { index, contactViewModel ->

            Log.i("CDOT_VC",

                " [Contacts] Processed fullName:${ contactViewModel.fullName} , displayName:${ contactViewModel.displayName.value},contact.name:${ contactViewModel.contact.value?.name}"
            )
        }
        viewModelScope.launch {
            contactsList.postValue(list)
        }
        Log.i("CDOT_VC", " processMagicSearchResults=> [Contacts] Processed ${results.size} results")
        Log.i("CDOT_VC", " processMagicSearchResults=>  Processed ${list.size} unique results")
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
                        Log.i("CDOT_VC"," fetchSipContacts = Contacts saved locally successfully")
                    } else {
                        Log.w( " fetchSipContacts =>", "No contacts to save")
                    }
                } catch (e: Exception) {
                    Log.e("CDOT_VC"," fetchSipContacts => Error fetching contacts: ${e.message}", e)
                }
            }
        }
    }

    // Coroutine function to fetch SIP contacts
    private suspend fun fetchSipContactList(context: Context, domainVal: String): List<SipContact> {
        return withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                Log.e("CDOT_VC","  fetchSipContactList=> No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList() // Return empty list if no internet connection
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }

            val domain = "http://192.168.1.31/fs_webservice/WebService.asmx/Get_MobionNumber"
            Log.e("CDOT_VC","  fetchSipContactList=> api call ->> $domain")
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e("CDOT_VC"," fetchSipContactList=>  Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e("CDOT_VC"," fetchSipContactList=> Error: ${e.message}")
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
                        Log.e("CDOT_VC"," fetchSipContactList=> END_TAG->Name: ${currentContact.name}")
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

    fun fetchBcContacts(context: Context) {
        viewModelScope.launch {
            if (domainMain != null) {
                val contactsBc = withContext(Dispatchers.IO) {
                    if (corePreferences.getCurrentUserPhoneNumber == "9176066606") {
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
        Log.i("CDOT_VC", "updateBcSettingsContactsList => Search query: $query")

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
        Log.i("CDOT_VC", "  updateGroupSettingsContactsList=>  Group Settings Query is $query")

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
                        fetchGroupSettingsContacts1(context, domainMain)
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

    // Coroutine function to fetch group settings contacts
    private suspend fun fetchGroupSettingsContacts1(
        context: Context,
        domainVal: String
    ): List<GroupSettingsContact> {
        return withContext(Dispatchers.IO) {
            val number = coreContext.core.authInfoList[0].username
            Log.e("CDOT_VC"," fetchGroupSettingsContacts=> Number - $number")
            if (!isNetworkAvailable(context)) {
                Log.e("CDOT_VC"," fetchGroupSettingsContacts=> No internet connection")
                withContext(Dispatchers.Main) { isNetwork.value = true }
                return@withContext emptyList() // Return empty list or handle as you see fit
            } else {
                withContext(Dispatchers.Main) { isNetwork.value = false }
            }
            val domain =
                "http://192.168.1.31/fs_webservice/WebService.asmx/GroupSetting_View?number=$number"
            Log.i("CDOT_VC"," fetchGroupSettingsContacts=> group contact-api = $domain")
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseGroupSettingsXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e("CDOT_VC"," fetchGroupSettingsContacts=>  Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e("CDOT_VC","  fetchGroupSettingsContacts=> Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    // XML parsing function for group settings contacts
    private fun parseGroupSettingsXml(parser: XmlPullParser): List<GroupSettingsContact> {
        Log.i("CDOT_VC", " parseGroupSettingsXml=> parser start")
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
                            Log.e("CDOT_VC"," parseGroupSettingsXml=> Group Name - ${currentContact.groupName}" )
                        } else {
                            Log.e("CDOT_VC","  parseGroupSettingsXml=> Group Name - ${currentContact.groupName}")
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

    fun startCall(number: String) {
        Log.i("CDOT_VC", " startCall=> Audio Call")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            false // Disable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept =
            false // Disable video acceptance
        coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
        coreContext.core.isVideoDisplayEnabled = false
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number)
    }

    fun startVideoCall(number: String) {
        Log.i("CDOT_VC", " startVideoCall=>  video Call")
        Log.i("CDOT_VC", " startVideoCall=> before-> video enable: ${coreContext.core.isVideoEnabled}")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            true // Enable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
        coreContext.core.isVideoCaptureEnabled = true // Enable video capture
        coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
        Log.i("CDOT_VC", " startVideoCall=>  after-> video enable :  ${coreContext.core.isVideoEnabled}")
        val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(addrPbx)
    }

    fun startBcAudioCall(number: String) {
        Log.i("CDOT_VC", " startCall=> Audio Call")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            false // Disable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept =
            false // Disable video acceptance
        coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
        coreContext.core.isVideoDisplayEnabled = false
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number)
    }

    fun startBcVideoCall(number: String) {
        Log.i("CDOT_VC", " startVideoCall=>  video Call")
        Log.i("CDOT_VC", " startVideoCall=> before-> video enable: ${coreContext.core.isVideoEnabled}")
        coreContext.core.videoActivationPolicy.automaticallyInitiate =
            true // Enable video initiation
        coreContext.core.videoActivationPolicy.automaticallyAccept = true // Enable video acceptance
        coreContext.core.isVideoCaptureEnabled = true // Enable video capture
        coreContext.core.isVideoDisplayEnabled = true // Ensure video display is enabled
        coreContext.core.nativePreviewWindowId = null
        coreContext.core.nativeVideoWindowId = null
        Log.i("CDOT_VC", " startVideoCall=>  after-> video enable :  ${coreContext.core.isVideoEnabled}")
        // val addrPbx = "sip:$number@$domainMain"
        coreContext.startCall(number)
    }

    companion object SipContacts {
        var tempAllSipContacts: List<SipContact> = emptyList()
    }

    fun saveContacts(contactList: List<SipContact>, domainVal: String) {
        Log.i("CDOT_VC"," saveContacts=> contactList: size=> ${contactList.size}")
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

            Log.i("CDOT_VC"," saveContacts=> Friends: ${friend.name}")
        }
    }

}