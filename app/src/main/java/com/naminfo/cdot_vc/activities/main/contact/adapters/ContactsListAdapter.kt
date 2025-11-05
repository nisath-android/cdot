package com.naminfo.cdot_vc.activities.main.contact.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.adapters.SelectionListAdapter
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.ContactViewModel
import com.naminfo.cdot_vc.activities.main.viewmodels.ListTopBarViewModel
import com.naminfo.cdot_vc.databinding.ContactListCellBinding
import com.naminfo.cdot_vc.databinding.GenericListHeaderBinding
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.HeaderAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.core.Friend
import org.linphone.core.tools.Log

class ContactsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<ContactViewModel, RecyclerView.ViewHolder>(
    selectionVM,
    ContactDiffCallback()
),
    HeaderAdapter {
    val selectedContactEvent: MutableLiveData<Event<Friend>> by lazy {
        MutableLiveData<Event<Friend>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ContactListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.contact_list_cell,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ContactListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contactViewModel: ContactViewModel) {
            with(binding) {
                viewModel = contactViewModel
                lifecycleOwner = viewLifecycleOwner
                selectionListViewModel = selectionViewModel

                // Observe selection changes
                selectionViewModel.isEditionEnabled.observe(viewLifecycleOwner) {
                    position = bindingAdapterPosition
                }

                // Handle item click safely
                setClickListener {
                    // Always switch to main thread safely
                    lifecycleOwner?.lifecycleScope?.launch(Dispatchers.Main) {
                        try {
                            val friend = contactViewModel.contact.value
                            friend?.let {
                                Log.i(
                                    "CDOT_VC",
                                    "ContactsListAdapter -> Selected item in list changed: ${it.name}"
                                )
                                selectedContactEvent.value = Event(it)
                            }
                        } catch (e: Exception) {
                            Log.e("CDOT_VC", "Error selecting contact: ${e.message}", e)
                        }
                    }
                }

                executePendingBindings()
            }
        }
    }


    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val contact = getItem(position)
        val firstLetter = contact.fullName.firstOrNull().toString()
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemFirstLetter = getItem(previousPosition).fullName.firstOrNull().toString()
            !firstLetter.equals(previousItemFirstLetter, ignoreCase = true)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val contact = getItem(position)
        val firstLetter = AppUtils.getInitials(contact.fullName, 1)
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

private class ContactDiffCallback : DiffUtil.ItemCallback<ContactViewModel>() {
    override fun areItemsTheSame(
        oldItem: ContactViewModel,
        newItem: ContactViewModel
    ): Boolean {
        return oldItem.fullName.compareTo(newItem.fullName) == 0
    }

    override fun areContentsTheSame(
        oldItem: ContactViewModel,
        newItem: ContactViewModel
    ): Boolean {
        return true
    }
}
