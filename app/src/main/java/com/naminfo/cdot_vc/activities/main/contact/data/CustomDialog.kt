package com.naminfo.cdot_vc.activities.main.contact.data

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.contact.adapters.OptionsAdapter1
import com.naminfo.cdot_vc.databinding.DialogCustomRecyclerBinding

class CustomBcContactsDialog(
    context: Context,
    private val options: List<BcInfoContact>,
    private val onOptionSelected: (BcInfoContact) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogCustomRecyclerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCustomRecyclerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // Dialog Window Settings
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // RecyclerView Setup
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = OptionsAdapter1(options) { selectedOption ->
            onOptionSelected(selectedOption)
            dismiss() // Close dialog after selection
        }
        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
            ContextCompat.getDrawable(context, R.drawable.divider_line)?.let { setDrawable(it) }
        }
        binding.recyclerView.addItemDecoration(dividerItemDecoration)
        // Close Button
        binding.btnClose.setOnClickListener { dismiss() }
    }
}
