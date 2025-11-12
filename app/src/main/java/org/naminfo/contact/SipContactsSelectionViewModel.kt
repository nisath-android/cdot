package org.naminfo.contact

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.SearchResult
import org.linphone.mediastream.Log
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel.SipContact
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

open class SipContactsSelectionViewModel : ContactsSelectionViewModel() {
    private val _sipContactsLiveData = MutableLiveData<List<SipContact>>()
    val sipContactsLiveData: LiveData<List<SipContact>> get() = _sipContactsLiveData

    val sipContactsSelected1 = MutableLiveData<Boolean>()
    private val selectedSipContacts = MutableLiveData<MutableList<SipContact>>()

    init {
        selectedSipContacts.value = mutableListOf()
    }

    fun fetchSipContacts(context: Context, domain: String) {
        viewModelScope.launch {
            val contacts = fetchSipContactList(context, domain)
            _sipContactsLiveData.postValue(contacts)
        }
    }

    fun applySipContactFilter() {
        val query = filter.value.orEmpty().trim()
        val filteredContacts = if (query.isEmpty()) {
            sipContactsLiveData.value ?: emptyList()
        } else {
            sipContactsLiveData.value?.filter {
                it.name?.contains(query, ignoreCase = true) == true
            } ?: emptyList()
        }
        _sipContactsLiveData.postValue(filteredContacts)
    }

    fun toggleSelectionForSearchResult1(searchResult: SearchResult) {
        val address = searchResult.address
        if (address != null) {
            toggleSelectionForAddress(address)
        }
    }
    fun toggleSipContactSelection(contact: SipContact) {
        val list = selectedSipContacts.value ?: mutableListOf()
        if (list.contains(contact)) {
            list.remove(contact)
        } else {
            list.add(contact)
        }
        selectedSipContacts.value = list
    }

    fun getSelectedSipContacts(): List<SipContact> {
        return selectedSipContacts.value ?: emptyList()
    }

    private suspend fun fetchSipContactList(context: Context, domain: String): List<SipContact> {
        return withContext(Dispatchers.IO) {
            // Replace with your actual implementation
            val url = URL("http://$domain/fs_webservice/WebService.asmx/Get_MobionNumber")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.inputStream.use { inputStream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(InputStreamReader(inputStream))
                    parseSipXml(parser)
                }
            } catch (e: Exception) {
                Log.e("SipContacts", "Error fetching contacts: ${e.message}")
                emptyList()
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Parses XML and returns a list of SIP contacts.
     */
    private fun parseSipXml(parser: XmlPullParser): List<SipContact> {
        val contacts = mutableListOf<SipContact>()
        var eventType = parser.eventType
        var currentContact: SipContact? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Table" -> currentContact = SipContact()
                        "Name" -> currentContact?.name = parser.nextText()
                        "Mobile_No" -> currentContact?.mobileNumber = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "Table" && currentContact != null) {
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
}
