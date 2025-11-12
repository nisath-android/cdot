
package org.naminfo.activities.main.recordings.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.naminfo.R
import org.naminfo.activities.main.adapters.SelectionListAdapter
import org.naminfo.activities.main.recordings.data.RecordingData
import org.naminfo.activities.main.viewmodels.ListTopBarViewModel
import org.naminfo.databinding.GenericListHeaderBinding
import org.naminfo.databinding.RecordingListCellBinding
import org.naminfo.utils.*

class RecordingsListAdapter(
    selectionVM: ListTopBarViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : SelectionListAdapter<RecordingData, RecyclerView.ViewHolder>(
    selectionVM,
    RecordingDiffCallback()
),
    HeaderAdapter {

    private lateinit var videoSurface: TextureView

    fun setVideoTextureView(textureView: TextureView) {
        videoSurface = textureView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: RecordingListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.recording_list_cell,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    inner class ViewHolder(
        val binding: RecordingListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recording: RecordingData) {
            with(binding) {
                data = recording

                lifecycleOwner = viewLifecycleOwner

                // This is for item selection through ListTopBarFragment
                position = bindingAdapterPosition
                selectionListViewModel = selectionViewModel

                setClickListener {
                    if (selectionViewModel.isEditionEnabled.value == true) {
                        selectionViewModel.onToggleSelect(bindingAdapterPosition)
                    }
                }

                setPlayListener {
                    if (recording.isPlaying.value == true) {
                        recording.pause()
                    } else {
                        recording.play()
                        if (recording.isVideoAvailable()) {
                            recording.setTextureView(videoSurface)
                        }
                    }
                }

                executePendingBindings()
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false

        val recording = getItem(position)
        val date = recording.date
        val previousPosition = position - 1

        return if (previousPosition >= 0) {
            val previousItemDate = getItem(previousPosition).date
            !TimestampUtils.isSameDay(date, previousItemDate)
        } else {
            true
        }
    }

    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        val recording = getItem(position)
        val date = formatDate(context, recording.date.time)
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header,
            null,
            false
        )
        binding.title = date
        binding.executePendingBindings()
        return binding.root
    }

    private fun formatDate(context: Context, date: Long): String {
        // Recordings is one of the few items in Linphone that is already in milliseconds
        if (TimestampUtils.isToday(date, false)) {
            return context.getString(R.string.today)
        } else if (TimestampUtils.isYesterday(date, false)) {
            return context.getString(R.string.yesterday)
        }
        return TimestampUtils.toString(
            date,
            onlyDate = true,
            timestampInSecs = false,
            shortDate = false,
            hideYear = false
        )
    }
}

private class RecordingDiffCallback : DiffUtil.ItemCallback<RecordingData>() {
    override fun areItemsTheSame(
        oldItem: RecordingData,
        newItem: RecordingData
    ): Boolean {
        return oldItem.compareTo(newItem) == 0
    }

    override fun areContentsTheSame(
        oldItem: RecordingData,
        newItem: RecordingData
    ): Boolean {
        return false // for headers
    }
}
