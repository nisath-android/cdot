package com.naminfo.cdot_vc.activities.main.contact.data

import androidx.lifecycle.MutableLiveData

class NumberOrAddressEditorData(val currentValue: String, val isSipAddress: Boolean) {
    val newValue = MutableLiveData<String>()

    val toRemove = MutableLiveData<Boolean>()

    init {
        newValue.value = currentValue
        toRemove.value = false
    }

    fun remove() {
        toRemove.value = true
    }
}

