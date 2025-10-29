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
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.utils.PermissionHelper

@TargetApi(34)
class Api34Compatibility {
    companion object {
        fun hasFullScreenIntentPermission(context: Context): Boolean {
            val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
            // See https://developer.android.com/reference/android/app/NotificationManager#canUseFullScreenIntent%28%29
            return notificationManager.canUseFullScreenIntent()
        }

        fun requestFullScreenIntentPermission(context: Context) {
            val intent = Intent()
            // See https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
            intent.action = Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            ContextCompat.startActivity(context, intent, null)
        }

        fun startCallForegroundService(
            service: Service,
            notifId: Int,
            notif: Notification,
            isCallActive: Boolean
        ) {
            val mask = if (isCallActive) {
                Log.i(
                    "[Api34 Compatibility] Trying to start service as foreground using at least FOREGROUND_SERVICE_TYPE_PHONE_CALL"
                )
                var computeMask = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                if (PermissionHelper.get().hasCameraPermission()) {
                    Log.i(
                        "[Api34 Compatibility] CAMERA permission has been granted, adding FOREGROUND_SERVICE_TYPE_CAMERA"
                    )
                    computeMask = computeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                if (PermissionHelper.get().hasRecordAudioPermission()) {
                    Log.i(
                        "[Api34 Compatibility] RECORD_AUDIO permission has been granted, adding FOREGROUND_SERVICE_TYPE_MICROPHONE"
                    )
                    computeMask = computeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                computeMask
            } else {
                Log.i(
                    "[Api34 Compatibility] Trying to start service as foreground using only FOREGROUND_SERVICE_TYPE_PHONE_CALL because call isn't active yet"
                )
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            }

            try {
                service.startForeground(
                    notifId,
                    notif,
                    mask
                )
            } catch (fssnae: ForegroundServiceStartNotAllowedException) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $fssnae")
            } catch (se: SecurityException) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $se")
            } catch (e: Exception) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $e")
            }
        }

        fun startDataSyncForegroundService(
            service: Service,
            notifId: Int,
            notif: Notification,
            isCallActive: Boolean
        ) {
            val mask = if (isCallActive) {
                Log.i(
                    "[Api34 Compatibility] Trying to start service as foreground using at least FOREGROUND_SERVICE_TYPE_PHONE_CALL or FOREGROUND_SERVICE_TYPE_DATA_SYNC"
                )
                var computeMask = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (PermissionHelper.get().hasCameraPermission()) {
                    Log.i(
                        "[Api34 Compatibility] CAMERA permission has been granted, adding FOREGROUND_SERVICE_TYPE_CAMERA"
                    )
                    computeMask = computeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                if (PermissionHelper.get().hasRecordAudioPermission()) {
                    Log.i(
                        "[Api34 Compatibility] RECORD_AUDIO permission has been granted, adding FOREGROUND_SERVICE_TYPE_MICROPHONE"
                    )
                    computeMask = computeMask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                computeMask
            } else {
                Log.i(
                    "[Api34 Compatibility] Trying to start service as foreground using only FOREGROUND_SERVICE_TYPE_DATA_SYNC because no call at the time"
                )
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }

            try {
                service.startForeground(
                    notifId,
                    notif,
                    mask
                )
            } catch (fssnae: ForegroundServiceStartNotAllowedException) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $fssnae")
            } catch (se: SecurityException) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $se")
            } catch (e: Exception) {
                Log.e("[Api34 Compatibility] Can't start service as foreground! $e")
            }
        }
    }
}
