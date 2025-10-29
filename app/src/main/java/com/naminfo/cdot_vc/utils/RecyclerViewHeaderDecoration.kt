package com.naminfo.cdot_vc.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewHeaderDecoration(private val context: Context, private val adapter: HeaderAdapter) : RecyclerView.ItemDecoration() {
    private val headers: SparseArray<View> = SparseArray()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = (view.layoutParams as RecyclerView.LayoutParams).bindingAdapterPosition

        if (position != RecyclerView.NO_POSITION && adapter.displayHeaderForPosition(position)) {
            val headerView: View = adapter.getHeaderViewForPosition(view.context, position)
            headers.put(position, headerView)
            measureHeaderView(headerView, parent)
            outRect.top = headerView.height
        } else {
            headers.remove(position)
        }
    }

    private fun measureHeaderView(view: View, parent: ViewGroup) {
        if (view.layoutParams == null) {
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.EXACTLY)
        val childWidth = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight,
            view.layoutParams.width
        )
        val childHeight = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )

        view.measure(childWidth, childHeight)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && adapter.displayHeaderForPosition(position)) {
                canvas.save()
                val headerView: View = headers.get(position) ?: adapter.getHeaderViewForPosition(
                    context,
                    position
                )
                canvas.translate(0f, child.y - headerView.height)
                headerView.draw(canvas)
                canvas.restore()
            }
        }
    }
}

interface HeaderAdapter {
    fun displayHeaderForPosition(position: Int): Boolean

    fun getHeaderViewForPosition(context: Context, position: Int): View
}
