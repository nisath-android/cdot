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
package org.naminfo.activities.main.contact.fragments

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import org.linphone.core.Factory
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.SnackBarActivity
import org.naminfo.activities.main.MainActivity
import org.naminfo.activities.main.contact.adapters.BroadcastContactAdapter
import org.naminfo.activities.main.contact.adapters.ContactsListAdapter
import org.naminfo.activities.main.contact.adapters.GroupContactAdapter
import org.naminfo.activities.main.contact.adapters.OptionsAdapter.Companion.splitContacts
import org.naminfo.activities.main.contact.adapters.PbxContactsAdapter
import org.naminfo.activities.main.contact.data.BcInfoContact
import org.naminfo.activities.main.contact.data.BroadcastContact
import org.naminfo.activities.main.contact.data.CustomBcContactsDialog
import org.naminfo.activities.main.contact.data.CustomGroupContactsDialog
import org.naminfo.activities.main.contact.data.GroupInfoContact
import org.naminfo.activities.main.contact.data.GroupSettingsContact
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.activities.main.contact.viewmodels.MockContactList
import org.naminfo.activities.main.contact.viewmodels.SimpleContact
import org.naminfo.activities.main.fragments.MasterFragment
import org.naminfo.activities.navigateToContact
import org.naminfo.databinding.ContactMasterFragmentBinding
import org.naminfo.utils.AppUtils
import org.naminfo.utils.Event
import org.naminfo.utils.RecyclerViewHeaderDecoration

private const val TAG = "[Contact-MasterContactsFragment]"
class MasterContactsFragment : MasterFragment<ContactMasterFragmentBinding, ContactsListAdapter>() {

    override val dialogConfirmationMessageBeforeRemoval = R.plurals.contact_delete_dialog
    private lateinit var listViewModel: ContactsListViewModel
    private lateinit var pbxAdapter: PbxContactsAdapter
    private lateinit var groupAdapter: GroupContactAdapter
    private lateinit var bcAdapter: BroadcastContactAdapter
    private var sipUriToAdd: String? = null

    // private var editOnClick: Boolean = false
    private var contactIdToDisplay: String? = null

    override fun getLayoutId(): Int = R.layout.contact_master_fragment

    override fun onDestroyView() {
        binding.contactsList.adapter = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, " onViewCreated=>")
        binding.lifecycleOwner = viewLifecycleOwner

        listViewModel = ViewModelProvider(this)[ContactsListViewModel::class.java]
        binding.viewModel = listViewModel
        listViewModel.pbxContactsSelected.value = false
        binding.pbxContactsList.visibility = View.INVISIBLE
        /* Shared view model & sliding pane related */

        setUpSlidingPane(binding.slidingPane)

        useMaterialSharedAxisXForwardAnimation = false
        sharedViewModel.updateContactsAnimationsBasedOnDestination.observe(
            viewLifecycleOwner
        ) {
            it.consume { id ->
                val forward = when (id) {
                    R.id.dialerFragment, R.id.masterChatRoomsFragment -> false
                    else -> true
                }
                if (corePreferences.enableAnimations) {
                    val portraitOrientation =
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    val axis =
                        if (portraitOrientation) MaterialSharedAxis.X else MaterialSharedAxis.Y
                    enterTransition = MaterialSharedAxis(axis, forward)
                    reenterTransition = MaterialSharedAxis(axis, forward)
                    returnTransition = MaterialSharedAxis(axis, !forward)
                    exitTransition = MaterialSharedAxis(axis, !forward)
                }
            }
        }

        sharedViewModel.layoutChangedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                sharedViewModel.isSlidingPaneSlideable.value = binding.slidingPane.isSlideable
                if (binding.slidingPane.isSlideable) {
                    val navHostFragment =
                        childFragmentManager.findFragmentById(R.id.contacts_nav_container) as NavHostFragment
                    if (navHostFragment.navController.currentDestination?.id == R.id.emptyContactFragment) {
                        Log.i(
                            TAG,
                            " onViewCreated=>",
                            "layoutChangedEvent-> Foldable device has been folded, closing side pane with empty fragment"
                        )
                        binding.slidingPane.closePane()
                    }
                }
            }
        }

        /* End of shared view model & sliding pane related */

        _adapter = ContactsListAdapter(listSelectionViewModel, viewLifecycleOwner)
        binding.contactsList.setHasFixedSize(true)
        binding.contactsList.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext())
        binding.contactsList.layoutManager = layoutManager

        // Divider between items
        binding.contactsList.addItemDecoration(
            AppUtils.getDividerDecoration(requireContext(), layoutManager)
        )

        // Displays the first letter header
        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), adapter)
        binding.contactsList.addItemDecoration(headerItemDecoration)

        adapter.selectedContactEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { contact ->
                Log.i(
                    " onViewCreated=> selectedContactEvent -> Selected item in list changed: ${contact.name}"
                )
                sharedViewModel.selectedContact.value = contact
                corePreferences.getCallerName = contact.name
                (requireActivity() as MainActivity).hideKeyboard()
                navigateToContact()
                binding.slidingPane.openPane()

                /*if (editOnClick) {
                    navigateToContactEditor(sipUriToAdd, binding.slidingPane)
                    editOnClick = false
                    sipUriToAdd = null
                } else {

                }*/
            }
        }

        coreContext.contactsManager.fetchInProgress.observe(
            viewLifecycleOwner
        ) {
            listViewModel.fetchInProgress.value = it
        }
        val gson = Gson()
        listViewModel.contactsList.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Contacts] Contact List empty - [${it.isEmpty()}] ")
            val simpleList = it.map { contactViewModel ->
                val fullName = contactViewModel.fullName ?: contactViewModel.displayName.value ?: contactViewModel.contact.value?.name ?: contactViewModel.contact.value?.address?.displayName ?: ""
                val phoneNumber = contactViewModel.contact.value?.address?.username ?: MockContactList.parseSipUri(
                    contactViewModel.contact.value?.address?.asStringUriOnly()!!
                ).first
                    ?: "${contactViewModel.contact.value?.address?.asStringUriOnly()!!}"
                val sipAddressUri = contactViewModel.contact.value?.address?.asStringUriOnly() ?: MockContactList.makeUrl(
                    phoneNumber!!,
                    corePreferences.defaultDomain
                )

                SimpleContact(
                    phone = phoneNumber ?: "",
                    name = fullName ?: "",
                    sipAddress = sipAddressUri ?: ""
                )
            }
            corePreferences.sipContactsSaved = gson.toJson(simpleList)

            it.forEachIndexed { index, contactViewModel ->

                Log.i(
                    TAG,
                    " [Contacts] Processed \nfullName:${ contactViewModel.fullName} ," +
                        " displayName:${ contactViewModel.displayName.value},\n" +
                        "contact.name:${ contactViewModel.contact.value?.name},\n" +
                        "contact.username:${ contactViewModel.contact.value?.address?.username},\n" +
                        "contact.asStringUriOnly:${ contactViewModel.contact.value?.address?.asStringUriOnly()},\n" +
                        "contact.domain:${ contactViewModel.contact.value?.address?.domain},\n" +
                        "contact.displayName:${ contactViewModel.contact.value?.address?.displayName},\n" +
                        "contact.isSip:${ contactViewModel.contact.value?.address?.isSip}"
                )
            }

            if (it.isEmpty()) {
                listViewModel.fetchSipContacts(requireContext())
            }
            val id = contactIdToDisplay
            if (id != null) {
                val contact = coreContext.contactsManager.findContactById(id)
                if (contact != null) {
                    contactIdToDisplay = null
                    Log.i("[Contacts] Found matching contact [$contact] after callback")
                    adapter.selectedContactEvent.value = Event(contact)
                } else {
                    Log.w("[Contacts] No contact found matching id [$id] after callback")
                }
            }
            adapter.submitList(it)
            Log.i("[Contacts] Contact List Observe")
        }

        listViewModel.moreResultsAvailableEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                (requireActivity() as SnackBarActivity).showSnackBar(
                    R.string.contacts_ldap_query_more_results_available
                )
            }
        }

        // Add click listeners for the toggle buttons
        binding.setSipContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = true
            listViewModel.pbxContactsSelected.value = false
            listViewModel.groupContactsSelected.value = false
            listViewModel.bcContactsSelected.value = false
        }

        binding.setPbxContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = false
            listViewModel.pbxContactsSelected.value = true
            listViewModel.groupContactsSelected.value = false
            listViewModel.bcContactsSelected.value = false
        }

        binding.setGroupContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = false
            listViewModel.pbxContactsSelected.value = false
            listViewModel.groupContactsSelected.value = true
            listViewModel.bcContactsSelected.value = false
        }

        binding.setBcContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = false
            listViewModel.pbxContactsSelected.value = false
            listViewModel.groupContactsSelected.value = false
            listViewModel.bcContactsSelected.value = true
            binding.noContact.visibility = View.GONE
            Log.i(
                TAG,
                " Broadcast toggle selected->toggle = ${listViewModel.bcContactsSelected.value}"
            )
        }

        // Observe LiveData for SIP contacts
        listViewModel.sipContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.contactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            Log.i(TAG, " onViewCreated=>", "sipContactsSelected-> observe clearcache =true")
            listViewModel.updateContactsList(true)
        }

        // Observe LiveData for PBX contacts
        listViewModel.pbxContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.pbxContactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar1.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.noContact.visibility = View.GONE
        }

        // Observe LiveData for Group contacts
        listViewModel.groupContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.groupContactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar2.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.noContact.visibility = View.GONE
        }

        listViewModel.bcContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            Log.i(
                TAG,
                " Broadcast selected-> observe bcContactsSelected = ${listViewModel.bcContactsSelected.value}"
            )
            binding.bcContactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar3.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.noContact.visibility = View.GONE
            listViewModel.updateBcSettingsContactsList()
        }

        listViewModel.filter.observe(
            viewLifecycleOwner
        ) {
            Log.i(TAG, " onViewCreated=>", " filter observe clearcache =false")
            listViewModel.updateContactsList(false)
        }

        listViewModel.pbxSearchQuery.observe(
            viewLifecycleOwner
        ) {
            listViewModel.updatePbxContactsList(false)
        }

        listViewModel.groupSettingsSearchQuery.observe(viewLifecycleOwner) {
            listViewModel.updateGroupSettingsContactsList()
        }

        listViewModel.bcSearchQuery.observe(viewLifecycleOwner) {
            listViewModel.updateBcSettingsContactsList()
        }

        // Setup RecyclerViews
        setupPbxContactsRecyclerView()
        setupGroupContactsRecyclerView()
        setupBcContactsRecyclerView()

        // Observe PBX contacts LiveData
        listViewModel.pbxContactsLiveData.observe(viewLifecycleOwner) { contacts ->
            android.util.Log.d("[xxxAdapter]", "11--> ${contacts.size}")
            contacts.forEachIndexed { index, groupSettingsContact ->
                Log.i(
                    TAG,
                    " onViewCreated=>",
                    " pbxContactsLiveData-> PBX Contacts: ${groupSettingsContact.First_Name}"
                )
            }
            if (contacts.size > 0) {
                // val contactList = contacts.sortedBy { it.First_Name?.uppercase() }
//                val gson = Gson()
//                corePreferences.pbxContactsSaved = gson.toJson(contacts)
                pbxAdapter.submitList(contacts) // Update the adapter with the new list of contacts
            }
        }

        listViewModel.groupSettingsContactsLiveData.observe(viewLifecycleOwner) { contacts ->
            contacts.forEachIndexed { index, groupSettingsContact ->
                Log.i(
                    TAG,
                    "ContactsFragment",
                    "->Group Contacts: ${groupSettingsContact.groupName}"
                )
            }

            android.util.Log.d("[xxxAdapter]", "12--> ${contacts.size}")
            if (contacts.size > 0) {
                val contactList = contacts.sortedBy { it.groupName?.uppercase() }
                val gson = Gson()
                corePreferences.groupContactsSaved = gson.toJson(contactList)
                groupAdapter.submitList(contactList)
            }
        }

        listViewModel.bcContactsLiveData.observe(viewLifecycleOwner) { contacts ->
            contacts.forEachIndexed { index, broadcastContact ->
                Log.i(
                    TAG,
                    "ContactsFragment",
                    "Broadcast Contacts: ${broadcastContact.bcName}"
                )
            }

            Log.i("BroadcastAdapter", "Size of Broadcast Contacts: ${contacts.size}")
            if (contacts.size > 0) {
                val contactList = contacts.sortedBy { it.bcName?.uppercase() }
                bcAdapter.submitList(contactList)
            }
        }

        val id = arguments?.getString("id")
        val sipUri = arguments?.getString("sipUri")
        val addressString = arguments?.getString("address")
        arguments?.clear()

        if (id != null) {
            Log.i(
                TAG,
                "onViewCreated=> pbxContactsLiveData-> Found contact id parameter in arguments [$id]"
            )
            val contact = coreContext.contactsManager.findContactById(id)
            if (contact != null) {
                Log.i(
                    TAG,
                    " onViewCreated=> pbxContactsLiveData-> Found matching contact [${contact.name}]"
                )
                adapter.selectedContactEvent.value = Event(contact)
            } else {
                Log.w(
                    TAG,
                    " onViewCreated=> pbxContactsLiveData-> Matching contact not found yet, waiting for contacts updated callback"
                )
                contactIdToDisplay = id
            }
        } else if (sipUri != null) {
            Log.i(
                TAG,
                " onViewCreated=> pbxContactsLiveData-> Found sipUri parameter in arguments [$sipUri]"
            )
            sipUriToAdd = sipUri
            (activity as MainActivity).showSnackBar(
                R.string.contact_choose_existing_or_new_to_add_number
            )
            // editOnClick = true
        } else if (addressString != null) {
            val address = Factory.instance().createAddress(addressString)
            if (address != null) {
                Log.i(
                    TAG,
                    " onViewCreated=> pbxContactsLiveData->elseif Found friend SIP address parameter in arguments [${address.asStringUriOnly()}]"
                )
                val contact = coreContext.contactsManager.findContactByAddress(address)
                if (contact != null) {
                    Log.i("[Contacts] Found matching contact $contact")
                    adapter.selectedContactEvent.value = Event(contact)
                } else {
                    Log.w(
                        TAG,
                        " onViewCreated=> pbxContactsLiveData-> No matching contact found for SIP address [${address.asStringUriOnly()}]"
                    )
                }
            }
        }
    }

    override fun deleteItems(indexesOfItemToDelete: ArrayList<Int>) {
        val list = ArrayList<Friend>()
        var closeSlidingPane = false
        for (index in indexesOfItemToDelete) {
            val contact = adapter.currentList[index].contact.value
            if (contact != null) {
                list.add(contact)
            }

            if (contact == sharedViewModel.selectedContact.value) {
                closeSlidingPane = true
            }
        }
        // listViewModel.deleteContacts(list)

        if (!binding.slidingPane.isSlideable && closeSlidingPane) {
            Log.i(
                TAG,
                " deleteItems=> Currently displayed contact has been deleted, removing detail fragment"
            )
            // clearDisplayedContact()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(
            TAG,
            " onResume => Contacts onResume  call updateContactsList(clearcahe =true) method"
        )
        // listViewModel.fetchSipContacts(requireContext())
        listViewModel.updateContactsList(true)
        listViewModel.updateBcSettingsContactsList()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            val granted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.i(TAG, " [onRequestPermissionsResult] READ_CONTACTS permission granted")
                coreContext.fetchContacts()
            } else {
                Log.w(TAG, "[onRequestPermissionsResult] READ_CONTACTS permission denied")
            }
        } else if (requestCode == 1) {
            val granted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.i(TAG, " [onRequestPermissionsResult] WRITE_CONTACTS permission granted")
                listSelectionViewModel.isEditionEnabled.value = false
            } else {
                Log.w(TAG, "[onRequestPermissionsResult] WRITE_CONTACTS permission denied")
            }
        }
    }

    private fun setupPbxContactsRecyclerView() {
        pbxAdapter = PbxContactsAdapter(listViewModel, viewLifecycleOwner)

        val pbxLayoutManager = LinearLayoutManager(requireContext())
        binding.pbxContactsList.layoutManager = pbxLayoutManager
        binding.pbxContactsList.adapter = pbxAdapter
        binding.pbxContactsList.addItemDecoration(
            AppUtils.getDividerDecoration(requireContext(), pbxLayoutManager)
        )
        // Displays the first letter header
        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), pbxAdapter)
        binding.pbxContactsList.addItemDecoration(headerItemDecoration)
        listViewModel.fetchPbxContacts(requireContext())

        /*  listViewModel.pbxContactsLiveData.observe(viewLifecycleOwner) { contacts ->
              val gson = Gson()
              corePreferences.pbxContactsSaved = gson.toJson(contacts)
              pbxAdapter.submitList(contacts)
          }*/
    }

    private fun setupGroupContactsRecyclerView() {
        groupAdapter = GroupContactAdapter(
            onInfoClicked = { groupContact ->
                showInfoPopup(groupContact)
            },
            listViewModel
        )

        val groupLayoutManager = LinearLayoutManager(requireContext())
        binding.groupContactsList.layoutManager = groupLayoutManager
        binding.groupContactsList.adapter = groupAdapter
        binding.groupContactsList.addItemDecoration(
            AppUtils.getDividerDecoration(requireContext(), groupLayoutManager)
        )
        // Displays the first letter header
        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), groupAdapter)
        binding.groupContactsList.addItemDecoration(headerItemDecoration)
        listViewModel.fetchGroupSettingsContacts(requireContext())
    }

    private fun setupBcContactsRecyclerView() {
        bcAdapter = BroadcastContactAdapter(
            onInfoClicked = { bcContact ->
                Log.i("Info POP UP clicked")
                showInfoPopup1(bcContact)
            },
            listViewModel,
            viewLifecycleOwner
        )

        val bcAdapterManager = LinearLayoutManager(requireContext())
        binding.bcContactsList.layoutManager = bcAdapterManager
        binding.bcContactsList.adapter = bcAdapter
        binding.bcContactsList.addItemDecoration(
            AppUtils.getDividerDecoration(requireContext(), bcAdapterManager)
        )
        // Displays the first letter header
        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), bcAdapter)
        binding.bcContactsList.addItemDecoration(headerItemDecoration)
        listViewModel.fetchBcContacts(requireContext())
    }

    private fun showInfoPopup1(bcContact: BroadcastContact) {
        Log.i("ContactsFragment", "->Info POP UP1 ${bcContact.userDetails}")
        val groupContactInfoList = mutableListOf<BcInfoContact>()
        splitContacts(bcContact.userDetails ?: "").forEach { (name, number) ->
            groupContactInfoList.add(
                BcInfoContact(groupUserName = name, groupPhoneNumber = number)
            )
        }
        groupContactInfoList.forEach {
            Log.i("ContactsFragment", "Name: ${it.groupUserName}, Number: ${it.groupPhoneNumber}")
        }
        CustomBcContactsDialog(requireContext(), groupContactInfoList) { selectedOption ->
            Toast.makeText(requireContext(), "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showInfoPopup(groupContact: GroupSettingsContact) {
        Log.i("ContactsFragment", "->Info POP UP ${groupContact.userDetails}")
        /*   val formattedDetails = groupContact.userDetails?.replace(",", "\n")
           AlertDialog.Builder(requireContext())
               .setTitle("Group Info")
               .setMessage(formattedDetails)
               .setPositiveButton("OK", null)
               .show()*/
        val groupContactInfoList = mutableListOf<GroupInfoContact>()
        splitContacts(groupContact.moderate ?: "").forEach { (name, number) ->
            groupContactInfoList.add(
                GroupInfoContact(groupUserName = name, groupPhoneNumber = number, true)
            )
        }
        splitContacts(groupContact.userDetails ?: "").forEach { (name, number) ->
            groupContactInfoList.add(
                GroupInfoContact(groupUserName = name, groupPhoneNumber = number, false)
            )
        }
        CustomGroupContactsDialog(requireContext(), groupContactInfoList) { selectedOption ->
            Toast.makeText(requireContext(), "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
        }.show()
    }

    /*private fun showInfoPopup(groupContact: GroupSettingsContact) {
        // Inflate the custom popup layout
        val view = layoutInflater.inflate(R.layout.row_group_info, null)
        val container = view.findViewById<LinearLayout>(R.id.groupInfoContainer)

        // Split userDetails into individual entries
        val details = groupContact.userDetails?.split(",")

        // Populate the container with rows
        details?.forEachIndexed { index, detail ->
            val parts = detail.split("-").map { it.trim() }
            val name = parts.getOrNull(0) ?: "Unknown"
            val number = parts.getOrNull(1) ?: "Unknown"

            // Inflate the row layout
            val rowView = layoutInflater.inflate(R.layout.row_group_info, container, false)

            // Set values for index, name, and number
            rowView.findViewById<TextView>(R.id.tvIndex).text = "${index + 1}."
            rowView.findViewById<TextView>(R.id.tvName).text = name
            rowView.findViewById<TextView>(R.id.tvNumber).text = number

            // Add the row to the container
            container.addView(rowView)
        }

        // Show the dialog
        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("OK", null)
            .show()
    }*/
}
