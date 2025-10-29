package com.naminfo.cdot_vc.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import java.util.regex.Pattern

class PatternClickableSpan {
    private var patterns: ArrayList<SpannablePatternItem> = ArrayList()

    inner class SpannablePatternItem(
        var pattern: Pattern,
        var listener: SpannableClickedListener
    )

    interface SpannableClickedListener {
        fun onSpanClicked(text: String)
    }

    inner class StyledClickableSpan(var item: SpannablePatternItem) : ClickableSpan() {
        override fun onClick(widget: View) {
            val tv = widget as TextView
            val span = tv.text as Spanned
            val start = span.getSpanStart(this)
            val end = span.getSpanEnd(this)
            val text = span.subSequence(start, end)
            item.listener.onSpanClicked(text.toString())
        }
    }

    fun add(
        pattern: Pattern,
        listener: SpannableClickedListener
    ): PatternClickableSpan {
        patterns.add(SpannablePatternItem(pattern, listener))
        return this
    }

    fun build(editable: CharSequence?): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(editable)
        for (item in patterns) {
            val matcher = item.pattern.matcher(ssb)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val url = StyledClickableSpan(item)
                ssb.setSpan(url, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return ssb
    }
}
