package com.naminfo.cdot_vc.activities.voip.fragments

import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.activities.GenericFragment

abstract class GenericVideoPreviewFragment<T : ViewDataBinding> : GenericFragment<T>() {
    private var previewX: Float = 0f
    private var previewY: Float = 0f
    private var switchX: Float = 0f
    private var switchY: Float = 0f

    private var switchCameraImageView: ImageView? = null

    private val previewTouchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previewX = view.x - event.rawX
                previewY = view.y - event.rawY
                switchX = (switchCameraImageView?.x ?: 0f) - event.rawX
                switchY = (switchCameraImageView?.y ?: 0f) - event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate()
                    .x(event.rawX + previewX)
                    .y(event.rawY + previewY)
                    .setDuration(0)
                    .start()
                switchCameraImageView?.apply {
                    animate()
                        .x(event.rawX + switchX)
                        .y(event.rawY + switchY)
                        .setDuration(0)
                        .start()
                }
                true
            }
            else -> {
                view.performClick()
                false
            }
        }
    }

    protected fun setupLocalVideoPreview(localVideoPreview: TextureView, switchCamera: ImageView?) {
        switchCameraImageView = switchCamera
        localVideoPreview.setOnTouchListener(previewTouchListener)
        coreContext.core.nativePreviewWindowId = localVideoPreview
    }

    protected fun cleanUpLocalVideoPreview(localVideoPreview: TextureView) {
        localVideoPreview.setOnTouchListener(null)
    }
}
