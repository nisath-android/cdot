package com.naminfo.cdot_vc.utils

import android.Manifest
import android.content.Context
import com.naminfo.cdot_vc.compatibility.Compatibility
import org.linphone.core.tools.Log
import org.linphone.mediastream.Version
//import com.naminfo.cdot_vc.utils.SingletonHolder

/**
 * Helper methods to check whether a permission has been granted and log the result
 */
class PermissionHelper private constructor(private val context: Context) {
    companion object : SingletonHolder<PermissionHelper, Context>(::PermissionHelper)

    private fun hasPermission(permission: String): Boolean {
        val granted = Compatibility.hasPermission(context, permission)

        if (granted) {
            Log.d("[Permission Helper] Permission $permission is granted")
        } else {
            Log.w("[Permission Helper] Permission $permission is denied")
        }

        return granted
    }

    fun hasReadContactsPermission(): Boolean {
        return hasPermission(Manifest.permission.READ_CONTACTS)
    }

    fun hasWriteContactsPermission(): Boolean {
        return hasPermission(Manifest.permission.WRITE_CONTACTS)
    }

    fun hasReadPhoneStatePermission(): Boolean {
        return hasPermission(Manifest.permission.READ_PHONE_STATE)
    }

    fun hasReadPhoneStateOrPhoneNumbersPermission(): Boolean {
        return Compatibility.hasReadPhoneStateOrNumbersPermission(context)
    }

    fun hasReadExternalStoragePermission(): Boolean {
        return Compatibility.hasReadExternalStoragePermission(context)
    }

    fun hasWriteExternalStoragePermission(): Boolean {
        if (Version.sdkAboveOrEqual(Version.API29_ANDROID_10)) return true
        return hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun hasCameraPermission(): Boolean {
        return hasPermission(Manifest.permission.CAMERA)
    }

    fun hasRecordAudioPermission(): Boolean {
        return hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    fun hasPostNotificationsPermission(): Boolean {
        return Compatibility.hasPostNotificationsPermission(context)
    }
}
