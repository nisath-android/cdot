package org.naminfo.activities.main.contact.adapters

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.naminfo.R
import org.naminfo.activities.main.contact.data.PBXContactsTable
import org.naminfo.activities.main.contact.viewmodels.ContactsListViewModel
import org.naminfo.databinding.GenericListHeaderBinding
import org.naminfo.databinding.PbxContactItemBinding
import org.naminfo.utils.AppUtils
import org.naminfo.utils.Event
import org.naminfo.utils.HeaderAdapter
class PbxContactsAdapter(
    private val contactListViewModel: ContactsListViewModel,
    private val viewLifecycleOwner: LifecycleOwner
) : ListAdapter<PBXContactsTable, PbxContactsAdapter.ViewHolder>(DiffCallback()),
    HeaderAdapter {
    // For item selection handling
    val selectedContactEvent: MutableLiveData<Event<PBXContactsTable>> by lazy {
        MutableLiveData<Event<PBXContactsTable>>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<PbxContactItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.pbx_contact_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: PbxContactItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: PBXContactsTable) {
            with(binding) {
                this.contact = contact
                lifecycleOwner = viewLifecycleOwner

                // Click listener
                setClickListener {
                    contactListViewModel.startCall(contact.PBX.toString())
                }

                executePendingBindings()
            }
        }
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        if (position >= itemCount) return false
        val contact = getItem(position)
        val firstLetter = contact.First_Name?.firstOrNull().toString()
        val previousPosition = position - 1
        return if (previousPosition >= 0) {
            val previousItemFirstLetter = getItem(previousPosition).First_Name?.firstOrNull().toString()
            !firstLetter.equals(previousItemFirstLetter, ignoreCase = true)
        } else {
            true
        }
    }

    companion object {
        @JvmStatic
        @BindingAdapter("loadProfileInitials")
        fun ImageView.loadProfileInitials(name: String?) {
            this.post {
                if (!name.isNullOrEmpty()) {
                    val drawable = TextDrawable(context, name)
                    this.setImageDrawable(drawable)
                } else {
                    this.setImageResource(R.drawable.voip_single_contact_avatar) // Default placeholder
                }
            }
        }
    }
    override fun getHeaderViewForPosition(context: Context, position: Int): View {
        Log.i("PbxContactsAdapter", "getHeaderViewForPosition: $position")
        val contact = getItem(position)
        val firstLetter = "${contact.First_Name}"?.let {
            AppUtils.getInitials(
                it,
                1
            )
        }
        val binding: GenericListHeaderBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.generic_list_header,
            null,
            false
        )
        binding.title = firstLetter
        binding.executePendingBindings()
        return binding.root
    }

    private class DiffCallback : DiffUtil.ItemCallback<PBXContactsTable>() {
        override fun areItemsTheSame(
            oldItem: PBXContactsTable,
            newItem: PBXContactsTable
        ): Boolean {
            return oldItem.Mobile_No == newItem.Mobile_No
        }

        override fun areContentsTheSame(
            oldItem: PBXContactsTable,
            newItem: PBXContactsTable
        ): Boolean {
            return oldItem == newItem
        }
    }
}

// 212121
class TextDrawable(context: Context, text: String) : Drawable() {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#212121") // Profile background color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE // Initials color
        textSize = 40f // Text size for initials
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val initials = text.take(2).uppercase()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val radius = Math.min(bounds.width(), bounds.height()) / 2f

        // Draw background circle
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, backgroundPaint)

        // Calculate text size dynamically based on available space
        val adjustedTextSize = radius * 0.6f
        textPaint.textSize = adjustedTextSize

        // Draw the initials
        canvas.drawText(
            initials,
            bounds.exactCenterX(),
            bounds.exactCenterY() - ((textPaint.descent() + textPaint.ascent()) / 2),
            textPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/*class PbxContactsAdapter : ListAdapter<PbxContact, PbxContactsAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<PbxContactItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.pbx_contact_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: PbxContactItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(contact: PbxContact) {
            binding.contact = contact
            binding.executePendingBindings()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PbxContact>() {
        override fun areItemsTheSame(oldItem: PbxContact, newItem: PbxContact): Boolean {
            return oldItem.extensionNumber == newItem.extensionNumber
        }

        override fun areContentsTheSame(oldItem: PbxContact, newItem: PbxContact): Boolean {
            return oldItem == newItem
        }
    }
}
*/
