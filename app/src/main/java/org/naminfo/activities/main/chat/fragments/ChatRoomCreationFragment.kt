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
package org.naminfo.activities.main.chat.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.apache.commons.logging.Log
import org.naminfo.R
import org.naminfo.activities.GenericActivity
import org.naminfo.activities.main.chat.adapters.SipContactsAdapter
import org.naminfo.activities.main.chat.viewmodels.ChatRoomCreationViewModel
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.activities.navigateToChatRoom
import org.naminfo.databinding.ChatRoomCreationFragmentBinding
import org.naminfo.utils.AppUtils
import org.naminfo.utils.LinphoneUtils

private const val TAG = "==>>ChatRoomCreationFragmen"
class ChatRoomCreationFragment : SecureFragment<ChatRoomCreationFragmentBinding>() {
    private lateinit var viewModel: ChatRoomCreationViewModel
    private lateinit var adapter: SipContactsAdapter

    override fun getLayoutId(): Int = R.layout.chat_room_creation_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        // showBackPress()
        useMaterialSharedAxisXForwardAnimation = sharedViewModel.isSlidingPaneSlideable.value == false

        val createGroup = arguments?.getBoolean("createGroup") ?: false

        viewModel = ViewModelProvider(this)[ChatRoomCreationViewModel::class.java]
        viewModel.createGroupChat.value = createGroup

        viewModel.isEncrypted.value = sharedViewModel.createEncryptedChatRoom

        binding.viewModel = viewModel

        viewModel.fetchSipContacts(requireContext())

        viewModel.sipContactsSelected.value = true
        adapter = SipContactsAdapter(viewLifecycleOwner)
        binding.contactsList.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext())
        binding.contactsList.layoutManager = layoutManager

        // Divider between items
        binding.contactsList.addItemDecoration(
            AppUtils.getDividerDecoration(requireContext(), layoutManager)
        )

        binding.back.visibility = if ((requireActivity() as GenericActivity).isTablet()) View.INVISIBLE else View.VISIBLE

        binding.setAllContactsToggleClickListener {
            viewModel.sipContactsSelected.value = false
        }

        binding.setSipContactsToggleClickListener {
            viewModel.sipContactsSelected.value = true
        }

        viewModel.sipContactsLiveData.observe(
            viewLifecycleOwner
        ) {
            android.util.Log.d(TAG, "onViewCreated: [Chat Room Creation] Sip contacts updated")
            android.util.Log.d("[xxxAdapter]", "1--> ${it.size}")
            adapter.submitList(it)
        }

        /*viewModel.isEncrypted.observe(
            viewLifecycleOwner
        ) {
            adapter.setLimeCapabilityRequired(it)
        }*/

        viewModel.sipContactsSelected.observe(
            viewLifecycleOwner
        ) {
            viewModel.updateSipContactsList(true)
        }

        /*viewModel.selectedAddresses.observe(
            viewLifecycleOwner
        ) {
            adapter.updateSelectedAddresses(it)
        }*/

        viewModel.chatRoomCreatedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatRoom ->
                sharedViewModel.selectedChatRoom.value = chatRoom
                navigateToChatRoom(AppUtils.createBundleWithSharedTextAndFiles(sharedViewModel))
            }
        }

        viewModel.search.observe(
            viewLifecycleOwner
        ) {
            viewModel.updateSipContactsList(true)
        }

        adapter.selectedContactEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { searchResult ->
                /*if (createGroup) {
                    viewModel.toggleSelectionForSearchResult(searchResult)
                } else {

                }*/
                viewModel.createOneToOneChat(searchResult)
            }
        }

        // addParticipantsFromSharedViewModel()

        /*// Next button is only used to go to group chat info fragment
        binding.setNextClickListener {
            sharedViewModel.createEncryptedChatRoom = viewModel.isEncrypted.value == true
            sharedViewModel.chatRoomParticipants.value = viewModel.selectedAddresses.value
            navigateToGroupInfo()
        }*/

        /*viewModel.onMessageToNotifyEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { messageResourceId ->
                (activity as MainActivity).showSnackBar(messageResourceId)
            }
        }*/

        /*if (corePreferences.enableNativeAddressBookIntegration) {
            if (!PermissionHelper.get().hasReadContactsPermission()) {
                Log.i("[Chat Room Creation] Asking for READ_CONTACTS permission")
                requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), 0)
            }
        }*/
    }

    /*@Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 0) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.i("[Chat Room Creation] READ_CONTACTS permission granted")
                coreContext.fetchContacts()
            } else {
                Log.w("[Chat Room Creation] READ_CONTACTS permission denied")
            }
        }
    }

    private fun addParticipantsFromSharedViewModel() {
        val participants = sharedViewModel.chatRoomParticipants.value
        if (participants != null && participants.size > 0) {
            viewModel.selectedAddresses.value = participants
        }
    }*/

    override fun onResume() {
        super.onResume()

        viewModel.secureChatAvailable.value = LinphoneUtils.isEndToEndEncryptedChatAvailable()
    }
}
// [Contacts] Magic search contacts available
