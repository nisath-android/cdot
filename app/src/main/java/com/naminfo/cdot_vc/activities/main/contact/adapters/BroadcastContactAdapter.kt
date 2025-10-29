package com.naminfo.cdot_vc.activities.main.contact.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.contact.data.BroadcastContact
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.MasterContactsViewModel
import com.naminfo.cdot_vc.databinding.BcContactItemBinding
import com.naminfo.cdot_vc.databinding.GenericListHeaderBinding
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.HeaderAdapter


class BroadcastContactAdapter(
    private val onInfoClicked: (BroadcastContact) -> Unit,
    private val contactListViewModel: MasterContactsViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : ListAdapter<BroadcastContact, BroadcastContactAdapter.ViewHolder>(DiffCallback()),
    HeaderAdapter {
    // For item selection handling
    val selectedContactEvent: MutableLiveData<Event<BroadcastContact>> by lazy {
        MutableLiveData<Event<BroadcastContact>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<BcContactItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.bc_contact_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: BcContactItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: BroadcastContact) {
            binding.groupNameTextView.text = contact.bcName
            binding.groupNumber.text = contact.bcNumber
            binding.infoButton1.setOnClickListener { onInfoClicked(contact) }

            binding.videoButton.setOnClickListener {
                contactListViewModel.startBcVideoCall(
                    contact.bcNumber.toString()
                )
            }
            binding.audioButton.setOnClickListener {
                contactListViewModel.startBcAudioCall(
                    contact.bcNumber.toString()
                )
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val contact = getItem(position)
        val firstLetter = contact.bcName?.firstOrNull().toString()
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemFirstLetter =
                getItem(previousPosition).bcName?.firstOrNull().toString()
            !firstLetter.equals(previousItemFirstLetter, ignoreCase = true)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val contact = getItem(position)
        val firstLetter = AppUtils.getInitials(contact.bcName ?: "", 1)
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

    private class DiffCallback : DiffUtil.ItemCallback<BroadcastContact>() {
        override fun areItemsTheSame(
            oldItem: BroadcastContact,
            newItem: BroadcastContact
        ): Boolean {
            return oldItem.bcNumber == newItem.bcNumber
        }

        override fun areContentsTheSame(
            oldItem: BroadcastContact,
            newItem: BroadcastContact
        ): Boolean {
            return oldItem == newItem
        }
    }
}
