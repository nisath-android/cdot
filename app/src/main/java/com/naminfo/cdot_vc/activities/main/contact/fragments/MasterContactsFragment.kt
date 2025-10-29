package com.naminfo.cdot_vc.activities.main.contact.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.MasterContactsViewModel
import com.naminfo.cdot_vc.activities.main.fragments.MasterFragment
import com.naminfo.cdot_vc.databinding.FragmentMasterContactsBinding
import com.naminfo.cdot_vc.activities.main.contact.adapters.BroadcastContactAdapter
import com.naminfo.cdot_vc.activities.main.contact.adapters.ContactsListAdapter
import com.naminfo.cdot_vc.activities.main.contact.adapters.GroupContactAdapter
import com.naminfo.cdot_vc.activities.main.contact.adapters.OptionsAdapter1.Companion.splitContacts
import com.naminfo.cdot_vc.activities.main.contact.data.BcInfoContact
import com.naminfo.cdot_vc.activities.main.contact.data.BroadcastContact
import com.naminfo.cdot_vc.activities.main.contact.data.CustomBcContactsDialog
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.RecyclerViewHeaderDecoration
import org.linphone.core.Factory
import org.linphone.core.Friend
//import org.linphone.core.tools.Log
import com.google.gson.Gson
import com.naminfo.cdot_vc.activities.navigateToContact

class MasterContactsFragment : MasterFragment<FragmentMasterContactsBinding, ContactsListAdapter>()  {

    // private var editOnClick: Boolean = false
    private var contactIdToDisplay: String? = null
    private lateinit var listViewModel: MasterContactsViewModel
    private lateinit var groupAdapter: GroupContactAdapter
    private lateinit var bcAdapter: BroadcastContactAdapter
    private var sipUriToAdd: String? = null

    override fun getLayoutId(): Int = R.layout.fragment_master_contacts

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        listViewModel = ViewModelProvider(this)[MasterContactsViewModel::class.java]
        binding.viewModel = listViewModel

        setUpSlidingPane(binding.slidingPane)

        useMaterialSharedAxisXForwardAnimation = false

        sharedViewModel.layoutChangedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                sharedViewModel.isSlidingPaneSlideable.value = binding.slidingPane.isSlideable
                if (binding.slidingPane.isSlideable) {
                    val navHostFragment =
                        childFragmentManager.findFragmentById(R.id.contacts_nav_container) as NavHostFragment
                    if (navHostFragment.navController.currentDestination?.id == R.id.emptyContactFragment) {
                        Log.i("CDOT_VC","layoutChangedEvent-> Foldable device has been folded, closing side pane with empty fragment")
                        binding.slidingPane.closePane()
                    }
                }
            }
        }

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
                Log.i("CDOT_VC",
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

        listViewModel.contactsList.observe(
            viewLifecycleOwner
        ) {
            Log.i("CDOT_VC","[Contacts] Contact List empty - [${it.isEmpty()}] ")
            if (it.isEmpty()) {
                listViewModel.fetchSipContacts(requireContext())
            }
            val id = contactIdToDisplay
            if (id != null) {
                val contact = coreContext.contactsManager.findContactById(id)
                if (contact != null) {
                    contactIdToDisplay = null
                    Log.i("CDOT_VC","[Contacts] Found matching contact [$contact] after callback")
                    adapter.selectedContactEvent.value = Event(contact)
                } else {
                    Log.w("CDOT_VC","[Contacts] No contact found matching id [$id] after callback")
                }
            }
            adapter.submitList(it)
            Log.i("CDOT_VC","[Contacts] Contact List Observe")
        }

        binding.setSipContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = true
            listViewModel.groupContactsSelected.value = false
            listViewModel.bcContactsSelected.value = false
        }

        binding.setGroupContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = false
            listViewModel.groupContactsSelected.value = true
            listViewModel.bcContactsSelected.value = false
        }

        binding.setBcContactsToggleClickListener {
            listViewModel.sipContactsSelected.value = false
            listViewModel.groupContactsSelected.value = false
            listViewModel.bcContactsSelected.value = true
            //binding.noContact.visibility = View.GONE
        }

        // Observe LiveData for SIP contacts
        listViewModel.sipContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.contactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            Log.i("CDOT_VC"," onViewCreated=> sipContactsSelected-> observe clearcache =true")
            listViewModel.updateContactsList(true)
        }
        // Observe LiveData for Group contacts
        listViewModel.groupContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.groupContactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar2.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            //binding.noContact.visibility = View.GONE
        }

        listViewModel.bcContactsSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.bcContactsList.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            binding.searchBar3.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            //binding.noContact.visibility = View.GONE
            listViewModel.updateBcSettingsContactsList()
        }

        setupGroupContactsRecyclerView()
        setupBcContactsRecyclerView()


        listViewModel.filter.observe(
            viewLifecycleOwner
        ) {
            Log.i("CDOT_VC"," onViewCreated=> filter observe clearcache =false")
            listViewModel.updateContactsList(false)
        }

        listViewModel.groupSettingsSearchQuery.observe(viewLifecycleOwner) {
            listViewModel.updateGroupSettingsContactsList()
        }

        listViewModel.bcSearchQuery.observe(viewLifecycleOwner) {
            listViewModel.updateBcSettingsContactsList()
        }

        listViewModel.groupSettingsContactsLiveData.observe(viewLifecycleOwner) { contacts ->
            contacts.forEachIndexed { index, groupSettingsContact ->
                Log.i("CDOT_VC","ContactsFragment Group Contacts: ${groupSettingsContact.groupName}")
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
                Log.i("CDOT_VC","ContactsFragment Broadcast Contacts: ${broadcastContact.bcName}")
            }

            Log.i("CDOT_VC","BroadcastAdapter Size of Broadcast Contacts: ${contacts.size}")
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
            Log.i("CDOT_VC","onViewCreated=> pbxContactsLiveData-> Found contact id parameter in arguments [$id]")
            val contact = coreContext.contactsManager.findContactById(id)
            if (contact != null) {
                Log.i("CDOT_VC"," onViewCreated=> pbxContactsLiveData-> Found matching contact [${contact.name}]")
                adapter.selectedContactEvent.value = Event(contact)
            } else {
                Log.w("CDOT_VC"," onViewCreated=> pbxContactsLiveData-> Matching contact not found yet, waiting for contacts updated callback")
                contactIdToDisplay = id
            }
        } else if (sipUri != null) {
            Log.i("CDOT_VC"," onViewCreated=> pbxContactsLiveData-> Found sipUri parameter in arguments [$sipUri]")
            sipUriToAdd = sipUri
            (activity as MainActivity).showSnackBar(
                R.string.contact_choose_existing_or_new_to_add_number
            )
            // editOnClick = true
        } else if (addressString != null) {
            val address = Factory.instance().createAddress(addressString)
            if (address != null) {
                Log.i("CDOT_VC"," onViewCreated=> pbxContactsLiveData->elseif Found friend SIP address parameter in arguments [${address.asStringUriOnly()}]")
                val contact = coreContext.contactsManager.findContactByAddress(address)
                if (contact != null) {
                    Log.i("CDOT_VC","[Contacts] Found matching contact $contact")
                    adapter.selectedContactEvent.value = Event(contact)
                } else {
                    Log.w("CDOT_VC"," onViewCreated=> pbxContactsLiveData-> No matching contact found for SIP address [${address.asStringUriOnly()}]")
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
            Log.i("CDOT_VC"," deleteItems=> Currently displayed contact has been deleted, removing detail fragment")
            // clearDisplayedContact()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("CDOT_VC"," onResume => Contacts onResume  call updateContactsList(clearcahe =true) method")
        // listViewModel.fetchSipContacts(requireContext())
        listViewModel.updateContactsList(true)
        listViewModel.updateBcSettingsContactsList()
    }

    private fun setupGroupContactsRecyclerView() {
        groupAdapter = GroupContactAdapter(
            onInfoClicked = { groupContact ->
                //showInfoPopup(groupContact)
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
                Log.i("CDOT_VC","Info POP UP clicked")
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
        Log.i("CDOT_VC","ContactsFragment ->Info POP UP1 ${bcContact.userDetails}")
        val groupContactInfoList = mutableListOf<BcInfoContact>()
        splitContacts(bcContact.userDetails ?: "").forEach { (name, number) ->
            groupContactInfoList.add(
                BcInfoContact(groupUserName = name, groupPhoneNumber = number)
            )
        }
        groupContactInfoList.forEach {
            Log.i("CDOT_VC","ContactsFragment Name: ${it.groupUserName}, Number: ${it.groupPhoneNumber}")
        }
        CustomBcContactsDialog(requireContext(), groupContactInfoList) { selectedOption ->
            Toast.makeText(requireContext(), "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
        }.show()
    }



}