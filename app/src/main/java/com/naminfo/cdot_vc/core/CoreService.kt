package com.naminfo.cdot_vc.core

import android.content.Intent
import androidx.core.app.NotificationCompat
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.LinphoneApplication.Companion.ensureCoreExists
import com.naminfo.cdot_vc.R
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.CoreService

class CoreService : CoreService() {
    override fun onCreate() {
        super.onCreate()
        Log.i("[Service] Created")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("[Service] Starting, ensuring Core exists")

        if (corePreferences.keepServiceAlive) {
            Log.i("[Service] Starting as foreground to keep app alive in background")
            val contextCreated = ensureCoreExists(
                applicationContext,
                pushReceived = false,
                service = this,
                useAutoStartDescription = false
            )
            if (!contextCreated) {
                // Only start foreground notification if context already exists, otherwise context will do it itself
                coreContext.notificationsManager.startForegroundToKeepAppAlive(this, false)
            }
        } else if (intent?.extras?.get("StartForeground") == true) {
            Log.i("[Service] Starting as foreground due to device boot or app update")
            val contextCreated = ensureCoreExists(
                applicationContext,
                pushReceived = false,
                service = this,
                useAutoStartDescription = true,
                skipCoreStart = true
            )
            if (contextCreated) {
                coreContext.start()
            } else {
                // Only start foreground notification if context already exists, otherwise context will do it itself
                coreContext.notificationsManager.startForegroundToKeepAppAlive(this, true)
            }
            coreContext.checkIfForegroundServiceNotificationCanBeRemovedAfterDelay(5000)
        } else {
            ensureCoreExists(
                applicationContext,
                pushReceived = false,
                service = this,
                useAutoStartDescription = false
            )
        }

        coreContext.notificationsManager.serviceCreated(this)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun createServiceNotificationChannel() {
        // Done elsewhere
    }

    override fun showForegroundServiceNotification(isVideoCall: Boolean) {
        Log.i("[Service] Starting service as foreground")
        coreContext.notificationsManager.startCallForeground(this)
    }

    override fun hideForegroundServiceNotification() {
        Log.i("[Service] Stopping service as foreground")
        coreContext.notificationsManager.stopCallForeground()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (LinphoneApplication.contextExists()) {
            if (coreContext.core.callsNb > 0) {
                Log.w(
                    "[Service] Task removed but there is at least one active call, do not stop the Core!"
                )
            } else if (!corePreferences.keepServiceAlive) {
                if (coreContext.core.isInBackground) {
                    Log.i("[Service] Task removed, stopping Core")
                    coreContext.stop()
                } else {
                    Log.w("[Service] Task removed but Core is not in background, skipping")
                }
            } else {
                Log.i(
                    "[Service] Task removed but we were asked to keep the service alive, so doing nothing"
                )
            }
        }

        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        if (LinphoneApplication.contextExists()) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i("[Service] Stopping")
            coreContext.notificationsManager.serviceDestroyed()
        }
        super.onDestroy()
    }
}
