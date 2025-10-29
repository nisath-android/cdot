package com.naminfo.cdot_vc.activities.main.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.utils.Event

//import org.linphone.utils.Event

/* Helper for view models to notify user of a massage through a Snackbar */
open abstract class MessageNotifierViewModel : ViewModel() {
    val onMessageToNotifyEvent = MutableLiveData<Event<Int>>()
    //open abstract val onMessageToNotifyEvent: Any
}
