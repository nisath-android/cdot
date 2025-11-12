package org.naminfo.activities.main.ptt.pttrecents.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import org.linphone.core.RegistrationState
import org.naminfo.R
import org.naminfo.activities.main.ptt.pttrecents.PTTRecentsViewModel
import org.naminfo.databinding.PttRecentItemVoicesBinding
class PttRecentAdapter(
    private val viewModel: PTTRecentsViewModel,
    private val context: Context,
    private val items: List<PttRecentItem>,
    private val onItemClick: (PttRecentItem) -> Unit,
    private val onMessageIconClick: (PttRecentItem) -> Unit
) : RecyclerView.Adapter<PttRecentAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: PttRecentItemVoicesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PttRecentItem) {
            binding.tvName.text = item.name
            binding.tvTime.text = item.time
            viewModel.setRegistrationStatus(RegistrationState.Ok)
            // Coil image loading
            binding.imgProfile.load(item.profileImageUrl) {
                placeholder(R.drawable.ptt_person)
                error(R.drawable.ptt_person)
                crossfade(true)
                transformations(CircleCropTransformation())
            }
            binding.activeStatus.setImageResource(viewModel.registrationStatusDrawable.value!!)

            // Handle item click (entire row)
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            // Handle click on message icon
            binding.imgMessage.setOnClickListener {
                onMessageIconClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PttRecentItemVoicesBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
