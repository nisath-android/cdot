package com.naminfo.cdot_vc.activities.main.history

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
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.adapters.SelectionListAdapter
import com.naminfo.cdot_vc.activities.main.contact.viewmodels.MasterContactsViewModel
import com.naminfo.cdot_vc.activities.main.history.GroupedCallLogData
import com.naminfo.cdot_vc.activities.main.viewmodels.ListTopBarViewModel
import com.naminfo.cdot_vc.databinding.GenericListHeaderBinding
import com.naminfo.cdot_vc.databinding.HistoryListCellBinding
import com.naminfo.cdot_vc.utils.*
import org.linphone.core.tools.Log

class CallLogsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<GroupedCallLogData, RecyclerView.ViewHolder>(
    selectionVM,
    CallLogDiffCallback()
), HeaderAdapter {

    private val gson by lazy { Gson() }
    private val sipListType: Type = object : TypeToken<ArrayList<MasterContactsViewModel.SipContact>>() {}.type

    private val sipContactList: ArrayList<MasterContactsViewModel.SipContact> by lazy {
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
                val activity = itemView.context as? Activity
                val rootView = activity?.findViewById<View>(android.R.id.content)
                if (rootView != null) {
                    Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
                } else {
                    Log.w("CallLogsAdapter", "Root view not found for Snackbar")
                }
            } catch (e: Exception) {
                Log.e("CallLogsAdapter", "Failed to show Snackbar", e)
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
                }else{
                   // showSafeSnackbar("⚠️ No display name found")
                }
            }else{
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
                } else false
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
        } else true
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
            else -> TimestampUtils.toString(date, onlyDate = true, shortDate = false, hideYear = false)
        }
    }
}

private class CallLogDiffCallback : DiffUtil.ItemCallback<GroupedCallLogData>() {
    override fun areItemsTheSame(oldItem: GroupedCallLogData, newItem: GroupedCallLogData): Boolean {
        return oldItem.lastCallLogId == newItem.lastCallLogId
    }

    override fun areContentsTheSame(oldItem: GroupedCallLogData, newItem: GroupedCallLogData): Boolean {
        return oldItem.callLogs.size == newItem.callLogs.size &&
                oldItem.lastCallLogViewModel?.displayName?.value == newItem.lastCallLogViewModel?.displayName?.value
    }
}
