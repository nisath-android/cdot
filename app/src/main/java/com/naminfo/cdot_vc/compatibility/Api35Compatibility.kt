@file:Suppress("DEPRECATION")

package com.naminfo.cdot_vc.compatibility

import android.annotation.TargetApi
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.naminfo.cdot_vc.LinphoneApplication
import com.naminfo.cdot_vc.utils.PermissionHelper
import org.linphone.core.tools.Log
import java.util.concurrent.TimeUnit

@TargetApi(35)
class Api35Compatibility {
    companion object {

        /**
         * Check if the app can use full-screen intents (Android 14+)
         */
        fun hasFullScreenIntentPermission(context: Context): Boolean {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager?.canUseFullScreenIntent() ?: false
        }

        /**
         * Request the permission to show full-screen notifications
         */
        fun requestFullScreenIntentPermission(context: Context) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            ContextCompat.startActivity(context, intent, null)
        }

        /**
         * Android 15 tightened background restrictions further.
         * This safely starts a *call-related* foreground service with the required types.
         */
        fun startCallForegroundService(
            service: Service,
            notifId: Int,
            notif: Notification,
            isCallActive: Boolean
        ) {
            val mask = buildMask(isCallActive, forDataSync = false)
            try {
                Log.i("[Api35Compatibility]", "Starting call foreground service (mask=$mask)")
                service.startForeground(notifId, notif, mask)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.e("[Api35Compatibility]", "Foreground start not allowed", e)
                fallbackStart(service, notifId, notif)
            } catch (e: SecurityException) {
                Log.e("[Api35Compatibility]", "SecurityException while starting foreground", e)
                fallbackStart(service, notifId, notif)
            } catch (e: Exception) {
                Log.e("[Api35Compatibility]", "Exception while starting foreground", e)
                fallbackStart(service, notifId, notif)
            }
        }

        /**
         * Android 15 supports more precise service types for data sync, IoT, etc.
         */
        fun startDataSyncForegroundService(
            service: Service,
            notifId: Int,
            notif: Notification,
            isSyncActive: Boolean
        ) {
            val mask = buildMask(isSyncActive, forDataSync = true)
            try {
                Log.i("[Api35Compatibility]", "Starting data-sync foreground service (mask=$mask)")
                service.startForeground(notifId, notif, mask)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.e("[Api35Compatibility]", "Foreground start not allowed", e)
                fallbackStart(service, notifId, notif)
            } catch (e: Exception) {
                Log.e("[Api35Compatibility]", "Failed to start foreground service", e)
                fallbackStart(service, notifId, notif)
            }
        }

        /**
         * Builds the service mask dynamically based on permissions.
         */
        private fun buildMask(active: Boolean, forDataSync: Boolean): Int {
            var mask = if (forDataSync)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL

            if (active && !forDataSync) {
                // Add active call-related capabilities
                if (PermissionHelper.get().hasCameraPermission()) {
                    mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                if (PermissionHelper.get().hasRecordAudioPermission()) {
                    mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
            }

            if (forDataSync && PermissionHelper.get().hasNetworkPermission(LinphoneApplication.coreContext.context)) {
                mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            }

            return mask
        }

        /**
         * Android 15-safe fallback: if FGS start fails, schedule it via WorkManager
         */
        private fun fallbackStart(service: Service, notifId: Int, notif: Notification) {
            val ctx = service.applicationContext
            Log.w("[Api35Compatibility]", "Scheduling fallback WorkManager start")
            val work = OneTimeWorkRequestBuilder<RestartServiceWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueue(work)
        }
    }
}
