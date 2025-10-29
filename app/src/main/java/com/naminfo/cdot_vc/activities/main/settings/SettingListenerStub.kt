package com.naminfo.cdot_vc.activities.main.settings

open class SettingListenerStub : SettingListener {
    override fun onClicked() {}

    override fun onAccountClicked(identity: String) {}

    override fun onTextValueChanged(newValue: String) {}

    override fun onBoolValueChanged(newValue: Boolean) {}

    override fun onListValueChanged(position: Int) {}
}
