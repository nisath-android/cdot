package com.naminfo.cdot_vc.activities.main.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.utils.Event

class ListTopBarViewModel : ViewModel() {
    val isEditionEnabled = MutableLiveData<Boolean>()
    val isReply = MutableLiveData<Boolean>(false)
    val replyTextNew = MutableLiveData<String>("")
    val title = MutableLiveData<String>("")
    val message = MutableLiveData<String>("")

    val isSelectionNotEmpty = MutableLiveData<Boolean>()

    val selectAllEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val unSelectAllEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val deleteSelectionEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val selectedItems = MutableLiveData<ArrayList<Int>>()

    init {
        isEditionEnabled.value = false
        isSelectionNotEmpty.value = false
        selectedItems.value = arrayListOf()
    }

    fun onSelectAll(lastIndex: Int) {
        val list = arrayListOf<Int>()
        list.addAll(0.rangeTo(lastIndex))

        selectedItems.value = list
        isSelectionNotEmpty.value = list.isNotEmpty()
    }

    fun onUnSelectAll() {
        val list = arrayListOf<Int>()

        selectedItems.value = list
        isSelectionNotEmpty.value = false
    }

    fun onToggleSelect(position: Int) {
        val list = arrayListOf<Int>()
        list.addAll(selectedItems.value.orEmpty())
        if (list.contains(position)) {
            list.remove(position)
        } else {
            list.add(position)
        }

        isSelectionNotEmpty.value = list.isNotEmpty()
        selectedItems.value = list
    }
}
