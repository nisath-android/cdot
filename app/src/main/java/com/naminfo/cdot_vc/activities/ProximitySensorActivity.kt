package com.naminfo.cdot_vc.activities

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY
import org.linphone.core.tools.Log

abstract class ProximitySensorActivity : GenericActivity() {
    private lateinit var proximityWakeLock: PowerManager.WakeLock
    private var proximitySensorEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            Log.w(
                "[Proximity Sensor Activity] PROXIMITY_SCREEN_OFF_WAKE_LOCK isn't supported on this device!"
            )
        }

        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "$packageName;proximity_sensor"
        )
    }

    override fun onPause() {
        enableProximitySensor(false)

        super.onPause()
    }

    override fun onDestroy() {
        enableProximitySensor(false)

        super.onDestroy()
    }

    protected fun enableProximitySensor(enable: Boolean) {
        if (enable) {
            if (!proximitySensorEnabled) {
                Log.i(
                    "[Proximity Sensor Activity] Enabling proximity sensor (turning screen OFF when wake lock is acquired)"
                )
                if (!proximityWakeLock.isHeld) {
                    Log.i("[Proximity Sensor Activity] Acquiring PROXIMITY_SCREEN_OFF_WAKE_LOCK")
                    proximityWakeLock.acquire()
                }
                proximitySensorEnabled = true
            }
        } else {
            if (proximitySensorEnabled) {
                Log.i(
                    "[Proximity Sensor Activity] Disabling proximity sensor (turning screen ON when wake lock is released)"
                )
                if (proximityWakeLock.isHeld) {
                    Log.i(
                        "[Proximity Sensor Activity] Asking to release PROXIMITY_SCREEN_OFF_WAKE_LOCK next time sensor detects no proximity"
                    )
                    proximityWakeLock.release(RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                }
                proximitySensorEnabled = false
            }
        }
    }
}
