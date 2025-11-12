package org.naminfo.activities.main.contact.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.naminfo.R
import org.naminfo.activities.main.contact.data.GroupInfoContact
import org.naminfo.databinding.GroupContactDetailsItemBinding

class OptionsAdapter(
    private val options: List<GroupInfoContact>,
    private val onItemClick: (GroupInfoContact) -> Unit
) : RecyclerView.Adapter<OptionsAdapter.OptionViewHolder>() {

    inner class OptionViewHolder(val binding: GroupContactDetailsItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = GroupContactDetailsItemBinding.inflate(inflater, parent, false)
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val option = options[position]
        if (option.isModerator) {
            holder.binding.groupUserStatus.visibility = ViewGroup.VISIBLE
            holder.binding.groupUserStatus.text = "Moderator"
            holder.binding.groupUserStatus.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.dark_grey_color
                )
            )
            holder.binding.groupUserName.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.primary_dark_color
                )
            )
            holder.binding.groupUserPhone.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.primary_dark_color
                )
            )
        } else {
          /*  holder.binding.groupUserStatus.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.light_grey_color
                )
            )*/
            holder.binding.groupUserStatus.visibility = ViewGroup.GONE
            holder.binding.groupUserName.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.black_color
                )
            )
            holder.binding.groupUserPhone.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.black_color
                )
            )
        }
        holder.binding.groupUserName.text = option.groupUserName
        holder.binding.groupUserPhone.text = option.groupPhoneNumber

        // holder.itemView.setOnClickListener { onItemClick(option) }
    }

    override fun getItemCount(): Int = options.size
    companion object {
        fun splitContacts(contactString: String): List<Pair<String, String>> {
            // Regex pattern to match flexible phone number formats
            val pattern = """(.*?)-(\+?\d{2,15})""".toRegex()

            return pattern.findAll(contactString).map {
                val (name, number) = it.destructured
                name.replaceFirst(",", "").trim() to number.replaceFirst(",", "").trim()
            }.toList()
        }
    }
}
