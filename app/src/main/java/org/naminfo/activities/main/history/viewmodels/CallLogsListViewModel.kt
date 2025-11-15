package org.naminfo.activities.main.history.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.activities.main.history.data.GroupedCallLogData
import org.naminfo.contact.ContactsUpdatedListenerStub
import org.naminfo.utils.Event
import org.naminfo.utils.LinphoneUtils
import org.naminfo.utils.TimestampUtils

class CallLogsListViewModel : ViewModel() {
    val callLogs = MutableLiveData<List<GroupedCallLogData>>()

    val filter = MutableLiveData<CallLogsFilter>()

    val showConferencesFilter = MutableLiveData<Boolean>()

    val contactsUpdatedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallLogUpdated(core: Core, log: CallLog) {
            updateCallLogs()
        }
    }

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i("[Call Logs] Contacts have changed")
            contactsUpdatedEvent.value = Event(true)
        }
    }

    init {
        filter.value = CallLogsFilter.ALL
        updateCallLogs()

        showConferencesFilter.value = LinphoneUtils.isRemoteConferencingAvailable()

        coreContext.core.addListener(listener)
        coreContext.contactsManager.addListener(contactsUpdatedListener)
    }

    override fun onCleared() {
        callLogs.value.orEmpty().forEach(GroupedCallLogData::destroy)

        coreContext.contactsManager.removeListener(contactsUpdatedListener)
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    fun showAllCallLogs() {
        filter.value = CallLogsFilter.ALL
        updateCallLogs()
    }

    fun showOnlyMissedCallLogs() {
        filter.value = CallLogsFilter.MISSED
        updateCallLogs()
    }

    fun showOnlyConferenceCallLogs() {
        filter.value = CallLogsFilter.CONFERENCE
        updateCallLogs()
    }

    fun deleteCallLogGroup(callLog: GroupedCallLogData?) {
        if (callLog != null) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    fun deleteCallLogGroups(listToDelete: ArrayList<GroupedCallLogData>) {
        for (callLog in listToDelete) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    private fun computeCallLogs(
        callLogs: Array<CallLog>,
        missed: Boolean,
        conference: Boolean
    ): ArrayList<GroupedCallLogData> {
        android.util.Log.i("CalLogs", "computeCallLogs: ")
        var previousCallLogGroup: GroupedCallLogData? = null
        val list = arrayListOf<GroupedCallLogData>()

        for (callLog in callLogs) {
            if (callLog.toAddress.username != null) {
                if ((!missed && !conference) || (missed && LinphoneUtils.isCallLogMissed(callLog)) || (conference && callLog.wasConference())) {
                    if (previousCallLogGroup == null) {
                        previousCallLogGroup = GroupedCallLogData(callLog)
                    } else if (!callLog.wasConference() && // Do not group conference call logs
                        callLog.wasConference() == previousCallLogGroup.lastCallLog.wasConference() && // Check that both are of the same type, if one has a conf-id and not the other the equal method will return true !
                        previousCallLogGroup.lastCallLog.localAddress.weakEqual(
                                callLog.localAddress
                            ) &&
                        previousCallLogGroup.lastCallLog.remoteAddress.equal(callLog.remoteAddress)
                    ) {
                        if (TimestampUtils.isSameDay(
                                previousCallLogGroup.lastCallLogStartTimestamp,
                                callLog.startDate
                            )
                        ) {
                            previousCallLogGroup.callLogs.add(callLog)
                            previousCallLogGroup.updateLastCallLog(callLog)
                        } else {
                            list.add(previousCallLogGroup)
                            previousCallLogGroup = GroupedCallLogData(callLog)
                        }
                    } else {
                        list.add(previousCallLogGroup)
                        previousCallLogGroup = GroupedCallLogData(callLog)
                    }
                }
            }
        }
        if (previousCallLogGroup != null && !list.contains(previousCallLogGroup)) {
            list.add(previousCallLogGroup)
        }

        return list
    }

    /*   private fun computeCallLogs(
           callLogs: Array<CallLog>,
           missed: Boolean,
           conference: Boolean
       ): ArrayList<GroupedCallLogData> {
           Log.i("CallLogs", "=== computeCallLogs START ===")
           Log.i("CallLogs", "Input: total=${callLogs.size}, missed=$missed, conference=$conference")

           var previousCallLogGroup: GroupedCallLogData? = null
           val list = arrayListOf<GroupedCallLogData>()

           callLogs.forEachIndexed { index, callLog ->
               if (callLog.toAddress.username != null) {
                   Log.i("CallLogs", "\n--- Processing index=$index ---")
                   Log.i(
                       "CallLogs",
                       "Log -> remote=${callLog.remoteAddress} local=${callLog.localAddress}"
                   )
                   Log.i(
                       "CallLogs",
                       "Start time: ${callLog.startDate}, Conference=${callLog.wasConference()}"
                   )

                   val isMissed = LinphoneUtils.isCallLogMissed(callLog)
                   Log.i("CallLogs", "isMissed=$isMissed")

                   val include =
                       (!missed && !conference) || // Normal mode
                           (missed && isMissed) || // Missed only
                           (conference && callLog.wasConference()) // Conference only

                   Log.i("CallLogs", "Should include this log? -> $include")

                   if (!include) {
                       Log.i("CallLogs", "SKIPPED this log")
                       return@forEachIndexed
                   }

                   // First log into group
                   if (previousCallLogGroup == null) {
                       Log.i("CallLogs", "Creating FIRST group with this call log")
                       previousCallLogGroup = GroupedCallLogData(callLog)
                       return@forEachIndexed
                   }

                   val previous = previousCallLogGroup!!

                   Log.i("CallLogs", "Comparing WITH previous group lastLog:")
                   Log.i("CallLogs", "previous.remote=${previous.lastCallLog.remoteAddress}")
                   Log.i("CallLogs", "previous.local=${previous.lastCallLog.localAddress}")
                   Log.i("CallLogs", "previous.conference=${previous.lastCallLog.wasConference()}")

                   val canGroup =
                       !callLog.wasConference() &&
                           callLog.wasConference() == previous.lastCallLog.wasConference() &&
                           previous.lastCallLog.localAddress.weakEqual(callLog.localAddress) &&
                           previous.lastCallLog.remoteAddress.equal(callLog.remoteAddress)

                   Log.i("CallLogs", "Can group with previous? -> $canGroup")

                   if (canGroup) {
                       val sameDay = TimestampUtils.isSameDay(
                           previous.lastCallLogStartTimestamp,
                           callLog.startDate
                       )

                       Log.i("CallLogs", "Is same day? -> $sameDay")

                       if (sameDay) {
                           Log.i("CallLogs", "Adding call to existing group")
                           previous.callLogs.add(callLog)
                           previous.updateLastCallLog(callLog)
                       } else {
                           Log.i("CallLogs", "Different day -> creating new group")
                           list.add(previous)
                           previousCallLogGroup = GroupedCallLogData(callLog)
                       }
                   } else {
                       Log.i("CallLogs", "CANNOT GROUP -> creating new group")
                       list.add(previous)
                       previousCallLogGroup = GroupedCallLogData(callLog)
                   }
               }
           }

           if (previousCallLogGroup != null && !list.contains(previousCallLogGroup)) {
               Log.i("CallLogs", "Adding FINAL group")
               list.add(previousCallLogGroup)
           }

           Log.i("CallLogs", "=== computeCallLogs END ===")
           Log.i("CallLogs", "Output groups count: ${list.size}")

           list.forEachIndexed { index, group ->
               Log.i(
                   "CallLogs",
                   "Group[$index] -> size=${group.callLogs.size},  last=${group.lastCallLog.remoteAddress}"
               )
           }

           return list
       }*/

    private fun updateCallLogs() {
        callLogs.value.orEmpty().forEach(GroupedCallLogData::destroy)

        val allCallLogs = coreContext.core.callLogs
        if (allCallLogs.isNotEmpty()) {
            allCallLogs.toList().map { callLog ->
                Log.i(
                    "CallLogsAdapter",
                    " [xxxCall Logs] ${allCallLogs.size} call logs found , username=${callLog.toAddress.username} startDate=${callLog.startDate} ,${callLog.toAddress.asStringUriOnly()}"
                )
            }
        }

        callLogs.value = when (filter.value) {
            CallLogsFilter.MISSED -> computeCallLogs(allCallLogs, missed = true, conference = false)
            CallLogsFilter.CONFERENCE -> computeCallLogs(
                allCallLogs,
                missed = false,
                conference = true
            )
            else -> computeCallLogs(allCallLogs, missed = false, conference = false)
        }
    }
}

enum class CallLogsFilter {
    ALL,
    MISSED,
    CONFERENCE
}
