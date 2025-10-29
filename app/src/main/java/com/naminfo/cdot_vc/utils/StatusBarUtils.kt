package com.naminfo.cdot_vc.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object StatusBarUtils {

    /**
     * Sets the system status bar background and icon appearance.
     *
     * Works seamlessly from API 26 to 35 (Android 8 → 15)
     */
    fun setStatusBar(
        activity: Activity,
        @ColorRes colorRes: Int,
        lightIcons: Boolean = false
    ) {
        val window = activity.window
        val decorView = window.decorView

        // ✅ Let system windows be drawn properly (no overlap)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // ✅ Controller (modern + backward compatible)
        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = lightIcons
        controller.show(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )

        val color = ContextCompat.getColor(activity, colorRes)

        if (Build.VERSION.SDK_INT >= 35) {
            // ✅ Android 15+ — use insets background instead of deprecated flags/colors
            val contentView = decorView.findViewById<View>(android.R.id.content)

            // Set background behind the status bar area
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                view.setPadding(0, statusBarInsets.top, 0, 0)
                view.setBackgroundColor(color)
                insets
            }

            contentView.setBackgroundColor(color)
            contentView.requestApplyInsets()

        } else {
            // ✅ API 26–34 — safe to set directly
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        }
    }

    /** Hide status bar for immersive mode */
    fun hideStatusBar(activity: Activity) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    /** Show status bar again */
    fun showStatusBar(activity: Activity) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.statusBars())
    }

    /** Toggle light/dark status bar icons dynamically */
    fun setLightStatusBarIcons(activity: Activity, lightIcons: Boolean) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = lightIcons
    }
}


