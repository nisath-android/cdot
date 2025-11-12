package org.naminfo.activities.main.contact.adapters

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
import org.naminfo.R
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.databinding.ContactListCell1Binding
import org.naminfo.databinding.GenericListHeaderBinding
import org.naminfo.utils.AppUtils
import org.naminfo.utils.Event
import org.naminfo.utils.HeaderAdapter

class SipContactsAdapter(
    private val viewModel: ContactsListViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : ListAdapter<ContactsListViewModel.SipContact, SipContactsAdapter.ViewHolder>(DiffCallback()),
    HeaderAdapter {

    val selectedContactEvent: MutableLiveData<Event<ContactsListViewModel.SipContact>> by lazy {
        MutableLiveData<Event<ContactsListViewModel.SipContact>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ContactListCell1Binding>(
            LayoutInflater.from(parent.context),
            R.layout.contact_list_cell1,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ContactListCell1Binding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: ContactsListViewModel.SipContact, position: Int) {
            with(binding) {
                this.lifecycleOwner = viewLifecycleOwner

                // this.contact = contact

                clickListener = View.OnClickListener {
                    selectedContactEvent.value = Event(contact)
                }

                this.position = position
                executePendingBindings()
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ContactsListViewModel.SipContact>() {
        override fun areItemsTheSame(
            oldItem: ContactsListViewModel.SipContact,
            newItem: ContactsListViewModel.SipContact
        ): Boolean {
            return oldItem.mobileNumber == newItem.mobileNumber
            // return
        }

        override fun areContentsTheSame(
            oldItem: ContactsListViewModel.SipContact,
            newItem: ContactsListViewModel.SipContact
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val contact = getItem(position)
        val firstLetter = contact.name?.firstOrNull().toString()
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemFirstLetter = getItem(previousPosition).name?.firstOrNull().toString()
            !firstLetter.equals(previousItemFirstLetter, ignoreCase = true)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val contact = getItem(position)
        val firstLetter = contact.name?.let { AppUtils.getInitials(it, 1) }
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
