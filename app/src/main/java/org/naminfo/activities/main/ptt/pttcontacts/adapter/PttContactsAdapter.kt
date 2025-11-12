package org.naminfo.activities.main.ptt.pttrecents.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import org.linphone.core.RegistrationState
import org.naminfo.R
import org.naminfo.activities.main.ptt.pttcontacts.PTTContactsViewModel
import org.naminfo.activities.main.ptt.pttcontacts.adapter.PttContactsItem
import org.naminfo.databinding.PttContactsItemBinding
class PttContactsAdapter(
    private val viewModel: PTTContactsViewModel,
    private val context: Context,
    private val items: List<PttContactsItem>,
    private val onItemClick: (PttContactsItem) -> Unit,
    private val onMessageIconClick: (PttContactsItem) -> Unit
) : RecyclerView.Adapter<PttContactsAdapter.PttContactsViewHolder>() {

    inner class PttContactsViewHolder(val binding: PttContactsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PttContactsItem) {
            binding.tvName.text = item.name
            binding.tvPttPhoneNumber.text = item.phone
            binding.imgMessage.visibility = View.GONE
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PttContactsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PttContactsItemBinding.inflate(inflater, parent, false)
        return PttContactsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PttContactsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
