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
package org.naminfo.activities.main.history.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication
import org.naminfo.R
import org.naminfo.activities.main.adapters.SelectionListAdapter
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.activities.main.history.data.GroupedCallLogData
import org.naminfo.activities.main.viewmodels.ListTopBarViewModel
import org.naminfo.databinding.GenericListHeaderBinding
import org.naminfo.databinding.HistoryListCellBinding
import org.naminfo.utils.*

class CallLogsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val rootView: View
) : SelectionListAdapter<GroupedCallLogData, RecyclerView.ViewHolder>(
    selectionVM,
    CallLogDiffCallback()
),
    HeaderAdapter {

    private val gson by lazy { Gson() }
    private val sipListType: Type = object : TypeToken<ArrayList<ContactsListViewModel.SipContact>>() {}.type

    private val sipContactList: ArrayList<ContactsListViewModel.SipContact> by lazy {
        try {
            gson.fromJson(
                LinphoneApplication.corePreferences.sipContactsSaved?.toString() ?: "[]",
                sipListType
            ) ?: arrayListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            arrayListOf()
        }
    }

    val selectedCallLogEvent = MutableLiveData<Event<GroupedCallLogData>>()
    val startCallToEvent = MutableLiveData<Event<GroupedCallLogData>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: HistoryListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.history_list_cell,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: HistoryListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private fun showSafeSnackbar(message: String) {
            try {
                // Try to get the Activity root first
                val activity = itemView.context as? Activity
                val rootView = activity?.window?.decorView?.findViewById<View>(android.R.id.content)

                val validParent = when {
                    rootView != null -> rootView
                    itemView.rootView != null -> itemView.rootView
                    else -> itemView
                }

                Snackbar
                    .make(validParent, message, Snackbar.LENGTH_SHORT)
                    .setAnchorView(
                        validParent.findViewById<View>(R.id.root_coordinator_layout) ?: validParent
                    )
                    .show()
            } catch (e: Exception) {
                Log.e("CallLogsAdapter", "⚠️ Snackbar error: ${e.message}", e)
            }
        }

        fun bind(callLogGroup: GroupedCallLogData) = with(binding) {
            val context = root.context
            val viewModel = callLogGroup.lastCallLogViewModel

            if (viewModel == null) {
                showSafeSnackbar("⚠️ Missing call log data")
                return
            }

            // Safe data binding
            this.viewModel = viewModel
            lifecycleOwner = viewLifecycleOwner
            selectionListViewModel = selectionViewModel

            // Observe only once (avoids multiple observers)
            selectionViewModel.isEditionEnabled.removeObservers(viewLifecycleOwner)
            selectionViewModel.isEditionEnabled.observe(viewLifecycleOwner) {
                position = bindingAdapterPosition
            }

            // Safe SIP display name replacement
            val displayName = viewModel.displayName?.value
            if (!displayName.isNullOrBlank() && displayName.startsWith("sip:", true)) {
                val callerName = LinphoneApplication.corePreferences.getCallerName
                if (!callerName.isNullOrBlank()) {
                    viewModel.displayName.value = callerName
                } else {
                    // showSafeSnackbar("⚠️ No display name found")
                }
            } else {
                // showSafeSnackbar("⚠️ No display name found")
            }

            // Match contact number from local SIP contact list
            sipContactList.firstOrNull { it.name == viewModel.displayName?.value }?.let {
                viewModel.contactNumber?.value = it.mobileNumber
            }

            // Click listener (single click)
            setClickListener {
                if (selectionViewModel.isEditionEnabled.value == true) {
                    selectionViewModel.onToggleSelect(bindingAdapterPosition)
                } else {
                    startCallToEvent.value = Event(callLogGroup)
                }
            }

            // Long press to enable multi-selection
            setLongClickListener {
                if (selectionViewModel.isEditionEnabled.value == false) {
                    selectionViewModel.isEditionEnabled.value = true
                    true
                } else {
                    false
                }
            }

            // Details button click
            setDetailsClickListener {
                if (selectionViewModel.isEditionEnabled.value == false) {
                    selectedCallLogEvent.value = Event(callLogGroup)
                }
            }

            groupCount = callLogGroup.callLogs.size
            executePendingBindings()
        }
    }

    // --- Header management ---
    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val date = getItem(position).lastCallLogStartTimestamp
        val prevPos = position - 1
        return if (prevPos >= 0) {
            val prevDate = getItem(prevPos).lastCallLogStartTimestamp
            !TimestampUtils.isSameDay(date, prevDate)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val callLog = getItem(position)
        val date = formatDate(context, callLog.lastCallLogStartTimestamp)
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header,
            null,
            false
        )
        binding.title = date
        binding.executePendingBindings()
        return binding.root
    }

    private fun formatDate(context: Context, date: Long): String {
        return when {
            TimestampUtils.isToday(date) -> context.getString(R.string.today)
            TimestampUtils.isYesterday(date) -> context.getString(R.string.yesterday)
            else -> TimestampUtils.toString(
                date,
                onlyDate = true,
                shortDate = false,
                hideYear = false
            )
        }
    }
}

private class CallLogDiffCallback : DiffUtil.ItemCallback<GroupedCallLogData>() {
    override fun areItemsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return oldItem.lastCallLogId == newItem.lastCallLogId
    }

    override fun areContentsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return oldItem.callLogs.size == newItem.callLogs.size &&
            oldItem.lastCallLogViewModel?.displayName?.value == newItem.lastCallLogViewModel?.displayName?.value
    }
}

/*class CallLogsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<GroupedCallLogData, RecyclerView.ViewHolder>(
    selectionVM,
    CallLogDiffCallback()
),
    HeaderAdapter {
    val gson: Gson by lazy { Gson() }

    val sipListType: Type =
        object : TypeToken<ArrayList<ContactsListViewModel.SipContact>>() {}.type
    val sipContactList: ArrayList<ContactsListViewModel.SipContact> = gson.fromJson(
        LinphoneApplication.corePreferences.sipContactsSaved?.toString() ?: "[]", // Default to an empty list
        sipListType
    ) ?: arrayListOf()
    val selectedCallLogEvent: MutableLiveData<Event<GroupedCallLogData>> by lazy {
        MutableLiveData<Event<GroupedCallLogData>>()
    }

    val startCallToEvent: MutableLiveData<Event<GroupedCallLogData>> by lazy {
        MutableLiveData<Event<GroupedCallLogData>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: HistoryListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.history_list_cell,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    inner class ViewHolder(
        val binding: HistoryListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(callLogGroup: GroupedCallLogData) {
            with(binding) {
                val callLogViewModel = callLogGroup.lastCallLogViewModel
                viewModel = callLogViewModel
                lifecycleOwner = viewLifecycleOwner
                // This is for item selection through ListTopBarFragment
                selectionListViewModel = selectionViewModel
                selectionViewModel.isEditionEnabled.observe(
                    viewLifecycleOwner
                ) {
                    position = bindingAdapterPosition
                }

                if (callLogGroup.lastCallLogViewModel.displayName.value.toString().startsWith(
                        "sip:",
                        0,
                        true
                    )
                ) {
                    callLogGroup.lastCallLogViewModel.displayName.value = LinphoneApplication.corePreferences.getCallerName
                }
                // var phoneNumber = 0
                sipContactList.forEach {
                    if (it.name == callLogGroup.lastCallLogViewModel.displayName.value) {
                        callLogGroup.lastCallLogViewModel.contactNumber.value = it.mobileNumber
                    }
                }
                setClickListener {
                    if (selectionViewModel.isEditionEnabled.value == true) {
                        selectionViewModel.onToggleSelect(bindingAdapterPosition)
                    } else {
                        startCallToEvent.value = Event(callLogGroup)
                    }
                }

                setLongClickListener {
                    if (selectionViewModel.isEditionEnabled.value == false) {
                        selectionViewModel.isEditionEnabled.value = true
                        // Selection will be handled by click listener
                        true
                    }
                    false
                }

                // This listener is disabled when in edition mode
                setDetailsClickListener {
                    selectedCallLogEvent.value = Event(callLogGroup)
                }

                groupCount = callLogGroup.callLogs.size

                executePendingBindings()
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val callLogGroup = getItem(position)
        val date = callLogGroup.lastCallLogStartTimestamp
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemDate = getItem(previousPosition).lastCallLogStartTimestamp
            !TimestampUtils.isSameDay(date, previousItemDate)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val callLog = getItem(position)
        val date = formatDate(context, callLog.lastCallLogStartTimestamp)
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header,
            null,
            false
        )
        binding.title = date
        binding.executePendingBindings()
        return binding.root
    }

    private fun formatDate(context: Context, date: Long): String {
        if (TimestampUtils.isToday(date)) {
            return context.getString(R.string.today)
        } else if (TimestampUtils.isYesterday(date)) {
            return context.getString(R.string.yesterday)
        }
        return TimestampUtils.toString(date, onlyDate = true, shortDate = false, hideYear = false)
    }
}

private class CallLogDiffCallback : DiffUtil.ItemCallback<GroupedCallLogData>() {
    override fun areItemsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return oldItem.lastCallLogId == newItem.lastCallLogId
    }

    override fun areContentsTheSame(
        oldItem: GroupedCallLogData,
        newItem: GroupedCallLogData
    ): Boolean {
        return oldItem.callLogs.size == newItem.callLogs.size
    }
}*/
