package com.naminfo.cdot_vc.activities.main.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.adapters.SelectionListAdapter
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.MasterContactsViewModel
import com.naminfo.cdot_vc.activities.main.history.GroupedCallLogData
import com.naminfo.cdot_vc.activities.main.viewmodels.ListTopBarViewModel
import com.naminfo.cdot_vc.databinding.GenericListHeaderBinding
import com.naminfo.cdot_vc.databinding.HistoryListCellBinding
import com.naminfo.cdot_vc.utils.*

class CallLogsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<GroupedCallLogData, RecyclerView.ViewHolder>(
    selectionVM,
    CallLogDiffCallback()
),
    HeaderAdapter {
    val gson: Gson by lazy { Gson() }

    val sipListType: Type =
        object : TypeToken<ArrayList<MasterContactsViewModel.SipContact>>() {}.type
    val sipContactList: ArrayList<MasterContactsViewModel.SipContact> = gson.fromJson(
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
}