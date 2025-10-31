package com.naminfo.cdot_vc.contact
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.utils.AppUtils


class ContactAvatarGenerator(private val context: Context) {
    private var textSize: Float
    private var textColor: Int
    private var avatarSize: Int
    private var name = " "
    private var backgroundColor: Int

    init {
        val theme = context.theme

        val backgroundColorTypedValue = TypedValue()
        theme.resolveAttribute(R.attr.primaryTextColor, backgroundColorTypedValue, true)
        backgroundColor = ContextCompat.getColor(context, backgroundColorTypedValue.resourceId)

        val textColorTypedValue = TypedValue()
        theme.resolveAttribute(R.attr.secondaryTextColor, textColorTypedValue, true)
        textColor = ContextCompat.getColor(context, textColorTypedValue.resourceId)

        textSize = AppUtils.getDimension(R.dimen.contact_avatar_text_size)

        avatarSize = AppUtils.getDimension(R.dimen.contact_avatar_size).toInt()
    }

    fun setTextSize(size: Float) = apply {
        textSize = size
    }

    fun setTextColorResource(resource: Int) = apply {
        textColor = ContextCompat.getColor(context, resource)
    }

    fun setAvatarSize(size: Int) = apply {
        avatarSize = size
    }

    fun setLabel(label: String) = apply {
        name = label
    }

    fun setBackgroundColorAttribute(attribute: Int) = apply {
        val theme = context.theme
        val backgroundColorTypedValue = TypedValue()
        theme.resolveAttribute(attribute, backgroundColorTypedValue, true)
        backgroundColor = ContextCompat.getColor(context, backgroundColorTypedValue.resourceId)
    }

    fun build(): BitmapDrawable {
        val label = AppUtils.getInitials(name)
        val textPainter = getTextPainter()
        val painter = getPainter()

        val bitmap = Bitmap.createBitmap(avatarSize, avatarSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val areaRect = Rect(0, 0, avatarSize, avatarSize)
        val bounds = RectF(areaRect)
        bounds.right = textPainter.measureText(label, 0, label.length)
        bounds.bottom = textPainter.descent() - textPainter.ascent()
        bounds.left += (areaRect.width() - bounds.right) / 2.0f
        bounds.top += (areaRect.height() - bounds.bottom) / 2.0f

        val halfSize = (avatarSize / 2).toFloat()
        canvas.drawCircle(halfSize, halfSize, halfSize, painter)
        canvas.drawText(label, bounds.left, bounds.top - textPainter.ascent(), textPainter)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun getTextPainter(): TextPaint {
        val textPainter = TextPaint()
        textPainter.isAntiAlias = true
        textPainter.textSize = textSize
        textPainter.color = textColor
        textPainter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        return textPainter
    }

    private fun getPainter(): Paint {
        val painter = Paint()
        painter.isAntiAlias = true
        painter.color = backgroundColor
        return painter
    }
}
