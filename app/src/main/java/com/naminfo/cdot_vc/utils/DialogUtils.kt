package com.naminfo.cdot_vc.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
//import androidx.databinding.DataBindingUtil
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
import com.naminfo.cdot_vc.databinding.DialogBinding
import com.naminfo.cdot_vc.databinding.VoipDialogBinding


class DialogUtils {
    companion object {
        fun getDialog(context: Context, viewModel: DialogViewModel): Dialog {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding: DialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog,
                null,
                false
            )
            binding.viewModel = viewModel
            dialog.setContentView(binding.root)

            val d: Drawable = ColorDrawable(
                ContextCompat.getColor(dialog.context, R.color.dark_grey_color)
            )
            d.alpha = 200
            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }

        fun getVoipDialog(context: Context, viewModel: DialogViewModel): Dialog {
            val dialog = Dialog(context, R.style.AppTheme)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding: VoipDialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.voip_dialog,
                null,
                false
            )
            binding.viewModel = viewModel
            dialog.setContentView(binding.root)

            val d: Drawable = ColorDrawable(
                ContextCompat.getColor(dialog.context, R.color.voip_dark_gray)
            )
            d.alpha = 166
            dialog.window
                ?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            dialog.window?.setBackgroundDrawable(d)
            return dialog
        }
    }
}
