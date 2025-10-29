package com.naminfo.cdot_vc.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.compatibility.Compatibility
import org.linphone.core.tools.Log


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true)) {
            val autoStart = corePreferences.autoStart
            Log.i("[Boot Receiver] Device is starting, autoStart is $autoStart")
            if (autoStart) {
                startService(context)
            }
        } else if (intent.action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true)) {
            val autoStart = corePreferences.autoStart
            Log.i("[Boot Receiver] App has been updated, autoStart is $autoStart")
            if (autoStart) {
                startService(context)
            }
        }
    }

    private fun startService(context: Context) {
        val serviceChannel = context.getString(R.string.notification_channel_service_id)
        val notificationManager = NotificationManagerCompat.from(context)
        if (Compatibility.getChannelImportance(notificationManager, serviceChannel) == NotificationManagerCompat.IMPORTANCE_NONE) {
            Log.w("[Boot Receiver] Service channel is disabled!")
            return
        }

        val serviceIntent = Intent(Intent.ACTION_MAIN).setClass(context, CoreService::class.java)
        serviceIntent.putExtra("StartForeground", true)
        Compatibility.startForegroundService(context, serviceIntent)
    }
}
