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
package org.naminfo.activities.assistant.viewmodels

import android.content.Context
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.use
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.activities.main.contact.data.ContactEditorData
import org.naminfo.activities.main.contact.data.NumberOrAddressEditorData
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.contact.NativeContactEditor
import org.naminfo.utils.Event
import org.naminfo.utils.PermissionHelper
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class GenericLoginViewModelFactory(private val accountCreator: AccountCreator) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GenericLoginViewModel(accountCreator) as T
    }
}

class GenericLoginViewModel(private val accountCreator: AccountCreator) : ViewModel() {

    private lateinit var data: ContactEditorData
    val username = MutableLiveData<String>()

    val password = MutableLiveData<String>()

    val domain = MutableLiveData<String>().apply {
        value = "192.168.1.50"
        // value = "103.16.202.169"
    }

    val displayName = MutableLiveData<String>()

    val transport = MutableLiveData<TransportType>()

    val loginEnabled: MediatorLiveData<Boolean> = MediatorLiveData()

    val waitForServerAnswer = MutableLiveData<Boolean>()

    val leaveAssistantEvent = MutableLiveData<Event<Boolean>>()

    /* private val dao: SipContactDao by lazy {
        // SipContactDatabaseInstance.getDatabase(LinphoneApplication.context).sipContactDao()
     }*/

    val invalidCredentialsEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val onErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private var accountToCheck: Account? = null

    // MutableLiveData to observe the CheckBox state
    val isChecked = MutableLiveData(false) // Initialize with default value false

    // Function to toggle the checkbox state
    fun toggleCheckbox() {
        isChecked.value = isChecked.value?.not() // Toggle the current value
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            // if (state?.name=="Ok")
            Log.i(
                "[Assistant] [Generic Login] ----------Registration state is $state: $message-----------"
            )
            if (account == accountToCheck) {
                Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    waitForServerAnswer.value = false
                    leaveAssistantEvent.value = Event(true)
                    core.removeListener(this)
                    Log.i(
                        "[Assistant] [Generic Login] Domain ${account.core.authInfoList.get(0).domain}"
                    )
                    // saveDomainToSharedPrefs(, account.core.authInfoList.get(0).domain)
                } else if (state == RegistrationState.Failed) {
                    waitForServerAnswer.value = false
                    invalidCredentialsEvent.value = Event(true)
                    core.removeListener(this)
                } /*else if (state == RegistrationState.Progress) {
                    waitForServerAnswer.value = false
                    invalidCredentialsEvent.value = Event(true)
                    core.removeListener(this)
                }*/
            }
        }
    }

    init {
        transport.value = TransportType.Udp

        loginEnabled.value = false
        loginEnabled.addSource(username) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(password) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(domain) {
            loginEnabled.value = isLoginButtonEnabled()
        }
    }

    fun setTransport(transportType: TransportType) {
        transport.value = transportType
    }

    fun removeInvalidProxyConfig() {
        val account = accountToCheck
        account ?: return

        val core = coreContext.core
        val authInfo = account.findAuthInfo()
        if (authInfo != null) core.removeAuthInfo(authInfo)
        core.removeAccount(account)
        accountToCheck = null

        // Make sure there is a valid default account
        val accounts = core.accountList
        if (accounts.isNotEmpty() && core.defaultAccount == null) {
            core.defaultAccount = accounts.first()
            core.refreshRegisters()
        }
    }

    fun continueEvenIfInvalidCredentials() {
        leaveAssistantEvent.value = Event(true)
    }

    fun createAccountAndAuthInfo() {
        coreContext.core.addListener(coreListener)
        Log.i("[Assistant] [Generic Login] Username - ${username.value}")
        if (username.value.isNullOrEmpty()) {
            waitForServerAnswer.value = false
            onErrorEvent.value = Event("Empty username not allowed")
            Log.i("[Assistant] [Generic Login] Username empty")
        } else {
            if (password.value.isNullOrEmpty()) {
                waitForServerAnswer.value = false
                onErrorEvent.value = Event("Empty password not allowed")
            } else if (domain.value.isNullOrEmpty()) {
                waitForServerAnswer.value = false
                onErrorEvent.value = Event("Empty domain not allowed")
            } else {
                waitForServerAnswer.value = true
                val domain = domain.value
                val loginDetails: ArrayList<Array<String>> = ArrayList()

                loginDetails.add(arrayOf("UserName", username.value.toString()))
                loginDetails.add(arrayOf("Password", password.value.toString()))

                // Launch a coroutine to handle the authentication asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    // val isAuthenticated = authenticate(loginDetails, domain)
                    withContext(Dispatchers.Main) {
                        /*if (isAuthenticated) {

                        } else {
                            Log.e("[Assistant] [Generic Login]", "Authentication failed.")
                            onErrorEvent.value = Event("Error: Authentication failed")
                        }*/
                        Log.i("[Assistant] [Generic Login]", " Authentication successful!")

                        // Only proceed to create the account if authenticated
                        accountCreator.username = username.value
                        accountCreator.password = password.value
                        accountCreator.domain = domain
                        accountCreator.displayName = displayName.value
                        accountCreator.transport = transport.value
                        corePreferences.getCurrentUserPhoneNumber = username.value
                        corePreferences.getCurrentUserName = displayName.value ?: username.value
                        Log.i(
                            "[Assistant] [Generic Login]",
                            "Authentication successful!-${displayName.value} | ${username.value}"
                        )

                        val account = accountCreator.createAccountInCore()
                        accountToCheck = account
                        Log.i(
                            "[Assistant] [Generic Login]",
                            "Authentication registeration state!-${account?.state} | ${username.value}"
                        )
                        if (account == null) {
                            Log.e(
                                "[Assistant] [Generic Login] Account creator couldn't create account"
                            )
                            coreContext.core.removeListener(coreListener)
                            onErrorEvent.value = Event("Error: Failed to create account object")
                        } else {
                            Log.i(
                                "[Assistant] [Generic Login] Account successfully created for domain: $domain"
                            )
                            coreContext.core.videoActivationPolicy.automaticallyInitiate =
                                true // Enable video initiation
                            coreContext.core.videoActivationPolicy.automaticallyAccept =
                                true // Enable video acceptance
                            coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                            coreContext.core.isVideoDisplayEnabled =
                                true // Ensure video display is enabled
                            Log.i(
                                "[Main Activity] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                            )
                        }
                        waitForServerAnswer.value = false
                    }
                }
            }
        }
    }

    // Suspending function for authentication
    private suspend fun authenticate(
        loginInfo: ArrayList<Array<String>>,
        domain: String?
    ): Boolean = suspendCoroutine { continuation ->
        val baseUrl = "http://$domain/fs_webservice/WebService.asmx/Mobion_Login"
        val queryString = loginInfo.joinToString("&") { param ->
            "${URLEncoder.encode(param[0], "UTF-8")}=${URLEncoder.encode(param[1], "UTF-8")}"
        }
        val fullUrl = "$baseUrl?$queryString"
        Log.i("[Assistant] [Generic Login]", "  URL for Login Auth: $fullUrl")

        val client = OkHttpClient()
        val request = Request.Builder().url(fullUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("[Assistant] [Generic Login]", "  Failed to authenticate: ${e.message}")
                continuation.resumeWith(Result.success(false)) // Resume coroutine with `false` on failure
            }

            override fun onResponse(call: okhttp3.Call, responses: Response) {
                responses.use {
                    Log.e(
                        "[Assistant] [Generic Login]",
                        "  Message:-> responses:$responses ,code:${responses.code} - message:${responses.message} - isSuccessful:${responses.isSuccessful}"
                    )
                    if (responses.isSuccessful) {
                        val response = responses.body?.string()

                        Log.e(
                            "[Assistant] [Generic Login]",
                            "  Message: $response "
                        )

                        when (responses.code) {
                            200 -> {
                                if (parseXml(response.toString())) {
                                    Log.i("[Assistant] [Generic Login]", " Response parseXml: true")
                                    continuation.resumeWith(Result.success(true))
                                } else {
                                    Log.i(
                                        "[Assistant] [Generic Login]",
                                        " Response parseXml: false"
                                    )
                                    continuation.resumeWith(Result.success(false))
                                }
                            }

                            else -> {
                                continuation.resumeWith(Result.success(false))
                            }
                        }
                    } else {
                        Log.e(
                            "[Assistant] [Generic Login]",
                            "  Error: ${responses.code} - ${responses.message}"
                        )
                        continuation.resumeWith(Result.success(false)) // Resume coroutine with `false` on error
                    }
                }
            }
        })
    }

    private fun getNodeValue(tag: String, element: Element): String {
        val nodeList = element.getElementsByTagName(tag)
        return if (nodeList.length > 0) nodeList.item(0).textContent else ""
    }

    fun parseXml(xmlString: String): Boolean {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlString))
            val document: Document = builder.parse(inputSource)

            // Normalize the XML structure
            document.documentElement.normalize()

            // Get the "Table" elements
            val tableList: NodeList = document.getElementsByTagName("Table")
            Log.i("[Generic Login]", " tableList.length: ${tableList.length}")
            Log.i(
                "[Generic Login]",
                " XML Content: ${document.documentElement.tagName}"
            )
            if (tableList.length == 0) {
                Log.i("[Generic Login]", "No Table elements found.")
                return false
            }
            for (i in 0 until tableList.length) {
                val tableNode = tableList.item(i) as Element
                Log.i("[********]", " Table Node: ${tableNode.tagName}")
                //  if (i == 0) {
                val firstName = getNodeValue("First_Name", tableNode)
                val lastName = getNodeValue("Last_Name", tableNode)
                val subscriberId = getNodeValue("Subscriber_ID", tableNode)
                val countryCode = getNodeValue("Country_Code", tableNode)
                val mobileNo = getNodeValue("Mobile_No", tableNode)
                val city = getNodeValue("City", tableNode)
                corePreferences.getCurrentUserPhoneNumber = mobileNo
                corePreferences.getCurrentUserName = "${firstName}$lastName"

                Log.i(
                    "[********]",
                    " First Name: $firstName"
                )
                Log.i(
                    "[********]",
                    " Last Name: $lastName"
                )
                Log.i(
                    "[********]",
                    " Subscriber ID: $subscriberId"
                )
                Log.i(
                    "[********]",
                    " Country Code: $countryCode"
                )
                Log.i(
                    "[********]",
                    " Mobile No: $mobileNo"
                )
                Log.i(
                    "[********]",
                    " City: $city"
                )
                return true
                // }
            }
        } catch (e: Exception) {
            // e.printStackTrace()
            Log.i(
                "[Generic Login]",
                " XML Content-Exception: ${e.message}"
            )
            return false
        }
        return false
    }

 /*   fun parseXml(xmlString: String): Boolean {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false // Ensure no namespace issues
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlString))
            val document: Document = builder.parse(inputSource)

            val root = document.documentElement
            Log.i("[Generic Login]", "Root Element: ${root.tagName}")

            // Ensure root element is correct
            if (root.tagName != "NewDataSet") {
                Log.i("[Generic Login]", "Unexpected root element: ${root.tagName}")
                return false
            }

            // Normalize XML structure
            document.documentElement.normalize()

            // Find "Table" elements
            val tableList: NodeList = document.getElementsByTagName("Table")
            Log.i("[Generic Login]", "Table count: ${tableList.length}")

            if (tableList.length == 0) {
                Log.i("[Generic Login]", "No Table elements found.")
                return false
            }

            // Process first "Table" element
            val tableNode = tableList.item(0) as Element
            val firstName = getNodeValue("First_Name", tableNode)
            val lastName = getNodeValue("Last_Name", tableNode)
            val subscriberId = getNodeValue("Subscriber_ID", tableNode)
            val countryCode = getNodeValue("Country_Code", tableNode)
            val mobileNo = getNodeValue("Mobile_No", tableNode)
            val city = getNodeValue("City", tableNode)

            // Save user info
            corePreferences.getCurrentUserPhoneNumber = mobileNo
            corePreferences.getCurrentUserName = "$firstName $lastName"

            // Print user details
            Log.i(
                "[Generic Login]",
                "User Details: First Name: $firstName, Last Name: $lastName, Subscriber ID: $subscriberId, Country Code: $countryCode, Mobile No: $mobileNo, City: $city"
            )

            return true
        } catch (e: Exception) {
            Log.e("[Generic Login]", "XML Parsing Error: ${e.message}")
            return false
        }
    }*/

    private fun isLoginButtonEnabled(): Boolean {
        return username.value.orEmpty().isNotEmpty() &&
            domain.value.orEmpty().isNotEmpty() &&
            password.value.orEmpty().isNotEmpty()
    }

    // Fetch SIP contacts function in ViewModel
    fun saveSipContacts(context: Context) {
        val domain1 = domain.value
        viewModelScope.launch {
            if (domain1 != null) {
                val contacts = fetchSipContactList(context, domain1!!)
                val allSipContacts = contacts

                if (allSipContacts.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val gson = Gson()
                        corePreferences.sipContactsSaved = gson.toJson(allSipContacts)
                    }
                    withContext(Dispatchers.Main) {
                        allSipContacts.forEachIndexed { index, sipContact ->
                            if (!corePreferences.getCurrentUserPhoneNumber.isNullOrEmpty()) {
                                if (corePreferences.getCurrentUserPhoneNumber == sipContact.mobileNumber) {
                                    Log.i(
                                        "[Assistant] [Generic Login]",
                                        "Contacts saved locally successfully=${sipContact.mobileNumber}:${sipContact.name}"
                                    )
                                    corePreferences.getCurrentUserName = sipContact.name
                                }
                            }
                        }
                    }

                    saveContacts(allSipContacts, domain1)
                    Log.i("LoginFragment", "Contacts saved locally successfully")
                } else {
                    Log.w("LoginFragment", "No contacts to save")
                }
            }
        }
    }

    // Coroutine function to fetch SIP contacts
    private suspend fun fetchSipContactList(
        context: Context,
        domainVal: String
    ): List<ContactsListViewModel.SipContact> {
        return withContext(Dispatchers.IO) {
            val domain = "http://$domain/fs_webservice/WebService.asmx/Get_MobionNumber"
            val url = URL(domain)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml(parser)
                }
            } catch (e: UnknownHostException) {
                Log.e("FetchSipContactsTask", "Host unreachable: ${e.message}")
                emptyList() // Handle error gracefully
            } catch (e: Exception) {
                Log.e("FetchSipContactsTask", "Error: ${e.message}")
                emptyList() // Handle other errors gracefully
            } finally {
                connection.disconnect()
            }
        }
    }

    // XML parsing function for SIP contacts
    private fun parseSipXml(parser: XmlPullParser): List<ContactsListViewModel.SipContact> {
        val contacts = mutableListOf<ContactsListViewModel.SipContact>()
        var eventType = parser.eventType
        var currentContact: ContactsListViewModel.SipContact? = null
        // var currentId = ""
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> {
                            currentContact = ContactsListViewModel.SipContact()
                            // currentId = parser.getAttributeValue(null, "diffgr:id") ?: ""
                        }

                        "Name" -> currentContact?.name = parser.nextText()
                        "Mobile_No" -> currentContact?.mobileNumber = parser.nextText()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
                        // Only add if name is not null
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

    private fun saveContacts(
        contactList: List<ContactsListViewModel.SipContact>,
        domainVal: String
    ) {
        for (sipContact in contactList) {
            val friend = coreContext.core.createFriend()
            friend.name = sipContact.name.orEmpty()
            Log.i("[Assistant] [Generic Login]", "Saving contact: ${sipContact.name}")
            // Save mobile number as a phone number
            sipContact.mobileNumber?.let { number ->
                friend.addPhoneNumber(number)
                val addr = Factory.instance().createAddress("sip:$number@$domainVal")
                friend.addAddress(addr!!)
            }
            coreContext.core.defaultFriendList?.addLocalFriend(friend)
        }
    }

    /*fun save(friend: ContactsListViewModel.SipContact, domainVal: String) {
        var contact = friend
        var created = false
        val fndcontact = coreContext.core.createFriend()

        val syncAccountName = "Mobion_FS"
        if (contact == null) {
            created = true
            // From Crashlytics it seems both permissions are required...

        }
        val nativeId = if (PermissionHelper.get().hasReadContactsPermission() &&
            PermissionHelper.get().hasWriteContactsPermission()
        ) {
            Log.i("[Contact Editor] Creating native contact")
            NativeContactEditor.createAndroidContact1(syncAccountName)
                .toString()
        } else {
            Log.e("[Contact Editor] Can't create native contact, permission denied")
            null
        }

        fndcontact.refKey = nativeId

        val mNumber = NumberOrAddressEditorData(contact.mobileNumber.toString(), false)
        val mNumberList: List<NumberOrAddressEditorData> = arrayListOf(mNumber)

        val addr = "sip:$mNumber@$domainVal"
        val mAddr = NumberOrAddressEditorData(addr, false)
        val addrList: List<NumberOrAddressEditorData> = arrayListOf(mAddr)

        if (fndcontact.refKey != null) {
            Log.i("[Contact Editor] Committing changes in native contact id ${fndcontact.refKey}")
            NativeContactEditor(fndcontact)
                .setFirstAndLastNames(contact.name!!, null.toString())
                .setPhoneNumbers(mNumberList)
                .setSipAddresses(addrList)
                .commit()
        }

        if (created) {
            coreContext.core.defaultFriendList?.addLocalFriend(fndcontact)
        } else {
            fndcontact.done()
        }
        Log.i("Friends: ${fndcontact.name}")
    }*/

    private fun save(friend: ContactsListViewModel.SipContact, domainVal: String) {
        val fndcontact = coreContext.core.createFriend()

        val nativeId = if (PermissionHelper.get().hasReadContactsPermission() &&
            PermissionHelper.get().hasWriteContactsPermission()
        ) {
            Log.i("[Contact Editor] Creating native contact for ${friend.name}")
            NativeContactEditor.createAndroidContact1(friend.name ?: "Unknown Contact").toString()
        } else {
            Log.e("[Contact Editor] Can't create native contact, permission denied")
            null
        }

        fndcontact.refKey = nativeId
        fndcontact.name = friend.name

        val mNumber = NumberOrAddressEditorData(friend.mobileNumber ?: "", false)
        val addr = "sip:${friend.mobileNumber}@$domainVal"

        Log.i("[Contact Editor] Mobile number: ${friend.mobileNumber}")
        Log.i("[Contact Editor] SIP address: sip:${friend.mobileNumber}@$domainVal")

        if (nativeId != null) {
            Log.i("[Contact Editor] Committing changes in native contact id $nativeId")
            NativeContactEditor(fndcontact)
                .setFirstAndLastNames(friend.name ?: "Unknown", null)
                .setPhoneNumbers(listOf(mNumber))
                .setSipAddresses(listOf(NumberOrAddressEditorData(addr, false)))
                .commit()
        }

        coreContext.core.defaultFriendList?.addLocalFriend(fndcontact)
        Log.i("[Contact Editor] Friend saved: ${fndcontact.name}")
    }
}
// Found 31 results in friends
