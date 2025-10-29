package com.naminfo.cdot_vc.activities.voip.views

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import com.naminfo.cdot_vc.R
import org.linphone.mediastream.video.capture.CaptureTextureView

class RoundCornersTextureView : CaptureTextureView {
    private var mRadius: Float = 0f

    constructor(context: Context) : super(context) {
        mAlignTopRight = true
        mDisplayMode = DisplayMode.BLACK_BARS
        setRoundCorners()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        readAttributes(attrs)
        setRoundCorners()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        readAttributes(attrs)
        setRoundCorners()
    }

    private fun readAttributes(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RoundCornersTextureView,
            0,
            0
        ).apply {
            try {
                mAlignTopRight = getBoolean(R.styleable.RoundCornersTextureView_alignTopRight, true)
                val mode = getInteger(
                    R.styleable.RoundCornersTextureView_displayMode,
                    DisplayMode.BLACK_BARS.ordinal
                )
                mDisplayMode = when (mode) {
                    1 -> DisplayMode.OCCUPY_ALL_SPACE
                    2 -> DisplayMode.HYBRID
                    else -> DisplayMode.BLACK_BARS
                }
                mRadius = getFloat(
                    R.styleable.RoundCornersTextureView_radius,
                    context.resources.getDimension(R.dimen.voip_round_corners_texture_view_radius)
                )
            } finally {
                recycle()
            }
        }
    }

    private fun setRoundCorners() {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rect = if (previewRectF != null &&
                    actualDisplayMode == DisplayMode.BLACK_BARS &&
                    mAlignTopRight
                ) {
                    Rect(
                        previewRectF.left.toInt(),
                        previewRectF.top.toInt(),
                        previewRectF.right.toInt(),
                        previewRectF.bottom.toInt()
                    )
                } else {
                    Rect(
                        0,
                        0,
                        width,
                        height
                    )
                }
                outline.setRoundRect(rect, mRadius)
            }
        }
        clipToOutline = true
    }

    override fun setAspectRatio(width: Int, height: Int) {
        super.setAspectRatio(width, height)

        val previewSize = previewVideoSize
        if (previewSize.width > 0 && previewSize.height > 0) {
            setRoundCorners()
        }
    }
}
