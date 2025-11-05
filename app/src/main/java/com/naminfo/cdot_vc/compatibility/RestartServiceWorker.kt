package com.naminfo.cdot_vc.compatibility

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.naminfo.cdot_vc.core.CoreService
import org.linphone.core.tools.Log

class RestartServiceWorker(appContext: Context, params: WorkerParameters) :
    Worker(appContext, params) {
    override fun doWork(): Result {
        return try {
            val intent = Intent(applicationContext, CoreService::class.java)
            ContextCompat.startForegroundService(applicationContext, intent)
            Log.i("[RestartServiceWorker]", "CoreService restarted successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("[RestartServiceWorker]", "Restart failed", e)
            Result.retry()
        }
    }
}
