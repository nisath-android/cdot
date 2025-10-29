package com.naminfo.cdot_vc.utils

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.service.AndroidDispatcher
//import org.linphone.core.tools.service.CoreManager

class ActivityMonitor : ActivityLifecycleCallbacks {
    private val activities = ArrayList<Activity>()
    private var mActive = false
    private var mRunningActivities = 0
    private var mLastChecker: InactivityChecker? = null

    @Synchronized
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!activities.contains(activity)) activities.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    @Synchronized
    override fun onActivityResumed(activity: Activity) {
        if (!activities.contains(activity)) {
            activities.add(activity)
        }
        mRunningActivities++
        checkActivity()
    }

    @Synchronized
    override fun onActivityPaused(activity: Activity) {
        if (!activities.contains(activity)) {
            activities.add(activity)
        } else {
            mRunningActivities--
            checkActivity()
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    @Synchronized
    override fun onActivityDestroyed(activity: Activity) {
        activities.remove(activity)
    }

    private fun startInactivityChecker() {
        if (mLastChecker != null) mLastChecker!!.cancel()
        AndroidDispatcher.dispatchOnUIThreadAfter(
            InactivityChecker().also { mLastChecker = it },
            2000
        )
    }

    private fun checkActivity() {
        if (mRunningActivities == 0) {
            if (mActive) startInactivityChecker()
        } else if (mRunningActivities > 0) {
            if (!mActive) {
                mActive = true
                onForegroundMode()
            }
            if (mLastChecker != null) {
                mLastChecker!!.cancel()
                mLastChecker = null
            }
        }
    }

    private fun onBackgroundMode() {
        coreContext.onBackground()
    }

    private fun onForegroundMode() {
        coreContext.onForeground()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    internal inner class InactivityChecker : Runnable {
        private var isCanceled = false
        fun cancel() {
            isCanceled = true
        }

        override fun run() {
//            if (CoreManager.isReady()) {
//                synchronized(CoreManager.instance()) {
//                    if (!isCanceled) {
//                        if (mRunningActivities == 0 && mActive) {
//                            mActive = false
//                            onBackgroundMode()
//                        }
//                    }
//                }
//            }
        }
    }
}
