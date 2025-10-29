package com.naminfo.cdot_vc.activities.voip.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.activities.voip.data.CallStatisticsData
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub

class StatisticsListViewModel : ViewModel() {
    val callStatsList = MutableLiveData<ArrayList<CallStatisticsData>>()

    private var enabled = false

    private val listener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            if (state == Call.State.End || state == Call.State.Error || state == Call.State.Connected) {
                computeCallsList()
            }
        }
    }

    init {
        coreContext.core.addListener(listener)

        computeCallsList()
    }

    fun enable() {
        enabled = true
        for (stat in callStatsList.value.orEmpty()) {
            stat.enable()
        }
    }

    fun disable() {
        enabled = false
        for (stat in callStatsList.value.orEmpty()) {
            stat.disable()
        }
    }

    override fun onCleared() {
        callStatsList.value.orEmpty().forEach(CallStatisticsData::destroy)
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    private fun computeCallsList() {
        callStatsList.value.orEmpty().forEach(CallStatisticsData::destroy)

        val list = arrayListOf<CallStatisticsData>()
        for (call in coreContext.core.calls) {
            if (call.state != Call.State.End && call.state != Call.State.Released && call.state != Call.State.Error) {
                val data = CallStatisticsData(call)
                list.add(data)
                if (enabled) {
                    data.enable()
                }
            }
        }

        callStatsList.value = list
    }
}
