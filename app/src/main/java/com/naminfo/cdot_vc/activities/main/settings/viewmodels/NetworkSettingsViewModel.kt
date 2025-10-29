package com.naminfo.cdot_vc.activities.main.settings.viewmodels

import androidx.lifecycle.MutableLiveData
import java.lang.NumberFormatException
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub

class NetworkSettingsViewModel : GenericSettingsViewModel() {
    val wifiOnlyListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            core.isWifiOnlyEnabled = newValue
        }
    }
    val wifiOnly = MutableLiveData<Boolean>()

    val allowIpv6Listener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            core.isIpv6Enabled = newValue
        }
    }
    val allowIpv6 = MutableLiveData<Boolean>()

    val randomPortsListener = object : SettingListenerStub() {
        override fun onBoolValueChanged(newValue: Boolean) {
            val port = 5061
            setTransportPort(port)
            sipPort.value = port
        }
    }
    val randomPorts = MutableLiveData<Boolean>()

    val sipPortListener = object : SettingListenerStub() {
        override fun onTextValueChanged(newValue: String) {
            try {
                val port = 5061
                setTransportPort(port)
            } catch (_: NumberFormatException) {
            }
        }
    }
    val sipPort = MutableLiveData<Int>()

    init {
        wifiOnly.value = core.isWifiOnlyEnabled
        allowIpv6.value = core.isIpv6Enabled
        randomPorts.value = getTransportPort() == -1
        sipPort.value = getTransportPort()
    }

    private fun setTransportPort(port: Int) {
        val transports = core.transports
        transports.udpPort = port
        transports.tcpPort = port
        transports.tlsPort = -1
        core.transports = transports
    }

    private fun getTransportPort(): Int {
        val transports = core.transports
        if (transports.udpPort > 0) return transports.udpPort
        return transports.tcpPort
    }
}
