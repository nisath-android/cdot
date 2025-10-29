package com.naminfo.cdot_vc.activities.main.history

import com.naminfo.cdot_vc.activities.main.contact.data.GroupSettingsContact
import com.naminfo.cdot_vc.activities.main.history.CallLogViewModel
import org.linphone.core.CallLog

class GroupedCallLogData(callLog: CallLog) {
    val callLogs = arrayListOf(callLog)
    var callType = "Contact"
    var dialPadNumber = ""
    var groupSettingContact: GroupSettingsContact? = null
    var lastCallLog: CallLog = callLog
    var lastCallLogId: String? = callLog.callId
    var lastCallLogStartTimestamp: Long = callLog.startDate
    val lastCallLogViewModel: CallLogViewModel
        get() {
            if (::_lastCallLogViewModel.isInitialized) {
                return _lastCallLogViewModel
            }
            _lastCallLogViewModel = CallLogViewModel(lastCallLog)
            return _lastCallLogViewModel
        }

    private lateinit var _lastCallLogViewModel: CallLogViewModel

    fun destroy() {
        if (::_lastCallLogViewModel.isInitialized) {
            lastCallLogViewModel
        }
    }

    fun updateLastCallLog(callLog: CallLog) {
        lastCallLog = callLog
        lastCallLogId = callLog.callId
        lastCallLogStartTimestamp = callLog.startDate
    }
}
