package com.naminfo.cdot_vc.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.naminfo.cdot_vc.LinphoneApplication.Companion.ensureCoreExists
import org.linphone.core.tools.Log

class CorePushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureCoreExists(context.applicationContext, true)
        Log.i("[Push Notification] Push notification has been received in broadcast receiver")
    }
}
