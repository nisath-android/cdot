package org.naminfo.activities.main.contact.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.linphone.core.tools.Log
import org.naminfo.R
import org.naminfo.activities.main.contact.data.GroupSettingsContact
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.databinding.GenericListHeaderBinding
import org.naminfo.databinding.GroupContactItemBinding
import org.naminfo.utils.AppUtils
import org.naminfo.utils.HeaderAdapter

private const val TAG = "==>>GroupContactAdapter"

class GroupContactAdapter(
    private val onInfoClicked: (GroupSettingsContact) -> Unit,
    private val viewModel: ContactsListViewModel
) : ListAdapter<GroupSettingsContact, GroupContactAdapter.ViewHolder>(DiffCallback()),
    HeaderAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<GroupContactItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.group_contact_item, // XML layout for the item view
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: GroupContactItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: GroupSettingsContact) {
            with(binding) {
                this.contact = contact
                groupNameTextView.text = contact.groupName
                groupNumber.text = contact.groupNumber
                /*if (contact.callType?.isNotEmpty() == true) {
                    callType.visibility = View.VISIBLE
                    callType.text = contact.callType
                    if (contact.callType == "Dial-IN") {
                        audioButton.isClickable = true
                        audioButton.isEnabled = true
                        videoButton.isClickable = true
                        videoButton.isEnabled = true
                        if (contact.conference == null || contact.conference == "" || contact.conference.isNullOrEmpty()) {
                            Log.i(TAG, "==>>Audio & Video invisible")
                            audioButton.visibility = View.GONE
                            videoButton.visibility = View.GONE
                        }
                        if (contact.conference == "Audio") {
                            Log.i(TAG, "==>>Audio Call visible")
                            audioButton.visibility = View.VISIBLE
                            videoButton.visibility = View.GONE
                        }
                        if (contact.conference == "Video") {
                            Log.i(TAG, "==>>Audio & Video visible")
                            audioButton.visibility = View.VISIBLE
                            videoButton.visibility = View.VISIBLE
                        }
                    } else if (contact.callType == "Dial-OUT") {
                        splitContacts(
                            contact.moderate ?: "${contact.groupName}-${contact.groupNumber}"
                        ).forEach { (name, number) ->
                            if (corePreferences.getCurrentUserPhoneNumber.toString()
                                .equals(number)
                            ) {
                                if (contact.moderate != null) {
                                    audioButton.isClickable = true
                                    audioButton.isEnabled = true
                                    videoButton.isClickable = true
                                    videoButton.isEnabled = true
                                    if (contact.conference == "") {
                                        Log.i(TAG, "==>>Audio & Video invisible")
                                        audioButton.visibility = View.GONE
                                        videoButton.visibility = View.GONE
                                    }
                                    if (contact.conference == "Audio") {
                                        Log.i(TAG, "==>>Audio Call visible")
                                        audioButton.visibility = View.VISIBLE
                                        videoButton.visibility = View.GONE
                                    }
                                    if (contact.conference == "Video") {
                                        Log.i(TAG, "==>>Audio & Video visible")
                                        audioButton.visibility = View.VISIBLE
                                        videoButton.visibility = View.VISIBLE
                                    }
                                } else {
                                    if (contact.conference == "") {
                                        Log.i(TAG, "==>>Audio & Video invisible")
                                        audioButton.visibility = View.GONE
                                        videoButton.visibility = View.GONE
                                    }
                                    if (contact.conference == "Audio") {
                                        Log.i(TAG, "==>>Audio Call visible")
                                        audioButton.isClickable = false
                                        audioButton.isEnabled = false
                                        audioButton.visibility = View.VISIBLE
                                        videoButton.visibility = View.GONE
                                    }
                                    if (contact.conference == "Video") {
                                        Log.i(TAG, "==>>Audio & Video visible")
                                        audioButton.visibility = View.VISIBLE
                                        videoButton.visibility = View.VISIBLE
                                        audioButton.isClickable = false
                                        audioButton.isEnabled = false
                                        videoButton.isClickable = false
                                        videoButton.isEnabled = false
                                    }
                                }
                            } else {
                                if (contact.conference == "") {
                                    Log.i(TAG, "==>>Audio & Video invisible")
                                    audioButton.visibility = View.GONE
                                    videoButton.visibility = View.GONE
                                }
                                if (contact.conference == "Audio") {
                                    Log.i(TAG, "==>>Audio Call visible")
                                    audioButton.isClickable = false
                                    audioButton.isEnabled = false
                                    audioButton.visibility = View.VISIBLE
                                    videoButton.visibility = View.GONE
                                }
                                if (contact.conference == "Video") {
                                    Log.i(TAG, "==>>Audio & Video visible")
                                    audioButton.visibility = View.VISIBLE
                                    videoButton.visibility = View.VISIBLE
                                    audioButton.isClickable = false
                                    audioButton.isEnabled = false
                                    videoButton.isClickable = false
                                    videoButton.isEnabled = false
                                }
                            }
                        }
                    }
                } else {
                    callType.visibility = View.GONE
                }*/

                callType.visibility = View.GONE
                infoButton.visibility = View.GONE
                // Set click listeners for each action
                /*val clickListener = View.OnClickListener { onInfoClicked(contact) }

                infoButton.setOnClickListener(clickListener)
                callType.setOnClickListener(clickListener)*/

                if (contact.conference == "Audio") {
                    Log.i(TAG, "==>>Audio Call visible")
                    audioButton.visibility = View.VISIBLE
                    videoButton.visibility = View.GONE
                } else if (contact.conference == "Video") {
                    Log.i(TAG, "==>>Video Call visible")
                    audioButton.visibility = View.GONE
                    videoButton.visibility = View.VISIBLE
                }
                videoButton.setOnClickListener {
                    // LinphoneApplication.corePreferences.isPttOutgoing = false
                    // LinphoneApplication.corePreferences.isPTT = false
                    viewModel.startVideoCall(
                        contact.groupNumber.toString()
                    )
                }
                audioButton.setOnClickListener {
                    // LinphoneApplication.corePreferences.isPttOutgoing = false
                    // LinphoneApplication.corePreferences.isPTT = false
                    viewModel.startCall(
                        contact.groupNumber.toString()
                    )
                }

                executePendingBindings()
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GroupSettingsContact>() {
        override fun areItemsTheSame(
            oldItem: GroupSettingsContact,
            newItem: GroupSettingsContact
        ): Boolean {
            return oldItem.id == newItem.id // Assuming `id` is a unique identifier for GroupSettingsContact
        }

        override fun areContentsTheSame(
            oldItem: GroupSettingsContact,
            newItem: GroupSettingsContact
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val contact = getItem(position)
        val firstLetter = contact.groupName?.firstOrNull().toString()
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemFirstLetter =
                getItem(previousPosition).groupName?.firstOrNull().toString()
            !firstLetter.equals(previousItemFirstLetter, ignoreCase = true)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val contact = getItem(position)
        val firstLetter = AppUtils.getInitials(contact.groupName ?: "", 1)
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header,
            null,
            false
        )
        binding.title = firstLetter
        binding.executePendingBindings()
        return binding.root
    }
}
