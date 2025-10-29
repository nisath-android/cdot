package com.naminfo.cdot_vc.activities.main.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.cdot_vc.activities.main.viewmodels.ListTopBarViewModel

abstract class SelectionListAdapter<T, VH : RecyclerView.ViewHolder>(
    selectionVM: ListTopBarViewModel,
    diff: DiffUtil.ItemCallback<T>
) :
    ListAdapter<T, VH>(diff) {

    private var _selectionViewModel: ListTopBarViewModel? = selectionVM
    protected val selectionViewModel get() = _selectionViewModel!!

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        _selectionViewModel = null
    }
}
