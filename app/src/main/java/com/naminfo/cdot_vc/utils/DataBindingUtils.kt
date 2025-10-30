/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.naminfo.cdot_vc.utils



import com.naminfo.cdot_vc.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.databinding.*

import androidx.lifecycle.LifecycleOwner
import coil.dispose
import coil.load
import coil.request.CachePolicy
import coil.request.videoFrameMillis
import coil.transform.CircleCropTransformation
import com.google.android.material.switchmaterial.SwitchMaterial
import com.naminfo.cdot_vc.BR
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.settings.SettingListener
import com.naminfo.cdot_vc.views.VoiceRecordProgressBar
import kotlinx.coroutines.*

import org.linphone.core.ConsolidatedPresence
import org.linphone.core.tools.Log


/**
 * This file contains all the data binding necessary for the app
 */

fun View.hideKeyboard() {
    try {
        val imm =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    } catch (_: Exception) {}
}

fun View.setKeyboardInsetListener(lambda: (visible: Boolean) -> Unit) {
    doOnLayout {
        var isKeyboardVisible = ViewCompat.getRootWindowInsets(this)?.isVisible(
            WindowInsetsCompat.Type.ime()
        ) == true

        lambda(isKeyboardVisible)

        // See https://issuetracker.google.com/issues/281942480
        ViewCompat.setOnApplyWindowInsetsListener(
            rootView
        ) { view, insets ->
            val keyboardVisibilityChanged = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            if (keyboardVisibilityChanged != isKeyboardVisible) {
                isKeyboardVisible = keyboardVisibilityChanged
                lambda(isKeyboardVisible)
            }
            ViewCompat.onApplyWindowInsets(view, insets)
        }
    }
}

@BindingAdapter("android:src")
fun ImageView.setSourceImageResource(resource: Int) {
    this.setImageResource(resource)
}

@BindingAdapter("android:contentDescription")
fun ImageView.setContentDescription(resource: Int) {
    if (resource == 0) {
        Log.w("Can't set content description with resource id 0")
        return
    }
    this.contentDescription = context.getString(resource)
}

@BindingAdapter("android:textStyle")
fun TextView.setTypeface(typeface: Int) {
    this.setTypeface(null, typeface)
}

@BindingAdapter("android:layout_size")
fun View.setLayoutSize(dimension: Float) {
    if (dimension == 0f) return
    this.layoutParams.height = dimension.toInt()
    this.layoutParams.width = dimension.toInt()
}

@BindingAdapter("backgroundImage")
fun LinearLayout.setBackgroundImage(resource: Int) {
    this.setBackgroundResource(resource)
}

@Suppress("DEPRECATION")
@BindingAdapter("style")
fun TextView.setStyle(resource: Int) {
    this.setTextAppearance(context, resource)
}

@BindingAdapter("android:layout_marginLeft")
fun setLeftMargin(view: View, margin: Float) {
    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
    layoutParams.leftMargin = margin.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_marginRight")
fun setRightMargin(view: View, margin: Float) {
    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
    layoutParams.rightMargin = margin.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_alignLeft")
fun setLayoutLeftAlign(view: View, oldTargetId: Int, newTargetId: Int) {
    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
    if (oldTargetId != 0) layoutParams.removeRule(RelativeLayout.ALIGN_LEFT)
    if (newTargetId != 0) layoutParams.addRule(RelativeLayout.ALIGN_LEFT, newTargetId)
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_alignRight")
fun setLayoutRightAlign(view: View, oldTargetId: Int, newTargetId: Int) {
    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
    if (oldTargetId != 0) layoutParams.removeRule(RelativeLayout.ALIGN_RIGHT)
    if (newTargetId != 0) layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, newTargetId)
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_toLeftOf")
fun setLayoutToLeftOf(view: View, oldTargetId: Int, newTargetId: Int) {
    val layoutParams = view.layoutParams as RelativeLayout.LayoutParams
    if (oldTargetId != 0) layoutParams.removeRule(RelativeLayout.LEFT_OF)
    if (newTargetId != 0) layoutParams.addRule(RelativeLayout.LEFT_OF, newTargetId)
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_gravity")
fun setLayoutGravity(view: View, gravity: Int) {
    val layoutParams = view.layoutParams as LinearLayout.LayoutParams
    layoutParams.gravity = gravity
    view.layoutParams = layoutParams
}

@BindingAdapter("layout_constraintGuide_percent")
fun setLayoutConstraintGuidePercent(guideline: Guideline, percent: Float) {
    val params = guideline.layoutParams as ConstraintLayout.LayoutParams
    params.guidePercent = percent
    guideline.layoutParams = params
}

@BindingAdapter("onClickToggleSwitch")
fun switchSetting(view: View, switchId: Int) {
    val switch: SwitchMaterial = view.findViewById(switchId)
    view.setOnClickListener { switch.isChecked = !switch.isChecked }
}

@BindingAdapter("onValueChanged")
fun editTextSetting(view: EditText, lambda: () -> Unit) {
    view.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            lambda()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter("onSettingImeDone")
fun editTextImeDone(view: EditText, lambda: () -> Unit) {
    view.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            lambda()

            view.clearFocus()

            val imm = view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            return@setOnEditorActionListener true
        }
        false
    }
}

@BindingAdapter("onFocusChangeVisibilityOf")
fun setEditTextOnFocusChangeVisibilityOf(editText: EditText, view: View) {
    editText.setOnFocusChangeListener { _, hasFocus ->
        view.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
    }
}

@BindingAdapter("selectedIndex", "settingListener")
fun spinnerSetting(spinner: Spinner, selectedIndex: Int, listener: SettingListener?) {
    spinner.setSelection(selectedIndex, true)

    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // From Crashlytics it seems this method may be called with a null listener
            listener?.onListValueChanged(position)
        }
    }
}

@BindingAdapter("onProgressChanged")
fun setListener(view: SeekBar, lambda: (Any) -> Unit) {
    view.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) lambda(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
}

@BindingAdapter("inflatedLifecycleOwner")
fun setInflatedViewStubLifecycleOwner(view: View, enable: Boolean) {
    val binding = DataBindingUtil.bind<ViewDataBinding>(view)
    // This is a bit hacky...
    if (view.context is LifecycleOwner) {
        binding?.lifecycleOwner = view.context as? LifecycleOwner
    }
}

@BindingAdapter("entries")
fun setEntries(
    viewGroup: ViewGroup,
    entries: List<ViewDataBinding>?
) {
    viewGroup.removeAllViews()
    if (entries != null) {
        for (i in entries) {
            viewGroup.addView(i.root)
        }
    }
}

private fun <T> setEntries(
    viewGroup: ViewGroup,
    entries: List<T>?,
    layoutId: Int,
    onLongClick: View.OnLongClickListener?,
    parent: Any?
) {
    viewGroup.removeAllViews()
    if (entries != null) {
        val inflater = viewGroup.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        for (entry in entries) {
            val binding = DataBindingUtil.inflate<ViewDataBinding>(
                inflater,
                layoutId,
                viewGroup,
                false
            )
            binding.setVariable(BR.data, entry)
            binding.setVariable(BR.longClickListener, onLongClick)
        //    binding.setVariable(BR.parent, parent)

            // This is a bit hacky...
            if (viewGroup.context is LifecycleOwner) {
                binding.lifecycleOwner = viewGroup.context as? LifecycleOwner
            }

            viewGroup.addView(binding.root)
        }
    }
}

@BindingAdapter("entries", "layout")
fun <T> setEntries(
    viewGroup: ViewGroup,
    entries: List<T>?,
    layoutId: Int
) {
    setEntries(viewGroup, entries, layoutId, null, null)
}

@BindingAdapter("entries", "layout", "onLongClick")
fun <T> setEntries(
    viewGroup: ViewGroup,
    entries: List<T>?,
    layoutId: Int,
    onLongClick: View.OnLongClickListener?
) {
    setEntries(viewGroup, entries, layoutId, onLongClick, null)
}

@BindingAdapter("entries", "layout", "parent")
fun <T> setEntries(
    viewGroup: ViewGroup,
    entries: List<T>?,
    layoutId: Int,
    parent: Any?
) {
    setEntries(viewGroup, entries, layoutId, null, parent)
}

@BindingAdapter("android:scaleType")
fun setImageViewScaleType(imageView: ImageView, scaleType: ImageView.ScaleType) {
    imageView.scaleType = scaleType
}

@BindingAdapter("coilRounded")
fun loadRoundImageWithCoil(imageView: ImageView, path: String?) {
    if (!path.isNullOrEmpty() && FileUtils.isExtensionImage(path)) {
        imageView.load(path) {
            transformations(CircleCropTransformation())
        }
    } else {
        Log.w("[Data Binding] [Coil] Can't load $path")
    }
}




@BindingAdapter("coilGoneIfError")
fun loadAvatarWithCoil(imageView: ImageView, path: String?) {
    if (path != null) {
        imageView.visibility = View.VISIBLE
        imageView.load(path) {
            transformations(CircleCropTransformation())
            listener(
                onError = { _, result ->
                    Log.e("[Data Binding] [Coil] Error loading [$path]: ${result.throwable}")
                    imageView.visibility = View.GONE
                },
                onSuccess = { _, _ ->
                    imageView.visibility = View.VISIBLE
                }
            )
        }
    } else {
        imageView.visibility = View.GONE
    }
}

@BindingAdapter("errorMessage")
fun setEditTextError(editText: EditText, error: String?) {
    if (error != editText.error) {
        editText.error = error
    }
}

@InverseBindingAdapter(attribute = "errorMessage")
fun getEditTextError(editText: EditText): String? {
    return editText.error?.toString()
}

@BindingAdapter("errorMessageAttrChanged")
fun setEditTextErrorListener(editText: EditText, attrChange: InverseBindingListener) {
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            attrChange.onChange()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            editText.error = null
            attrChange.onChange()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}


@BindingAdapter("coilVideoPreview")
fun loadVideoPreview(imageView: ImageView, path: String?) {
    if (!path.isNullOrEmpty() && FileUtils.isExtensionVideo(path)) {
        imageView.load(path) {
            videoFrameMillis(0)
            listener(
                onError = { _, result ->
                    Log.e(
                        "[Data Binding] [Coil] Error getting preview picture from video? [$path]: ${result.throwable}"
                    )
                },
                onSuccess = { _, _ ->
                    // Display "play" button above video preview
                    LayoutInflater.from(imageView.context).inflate(
                        R.layout.video_play_button,
                        imageView.parent as ViewGroup
                    )
                }
            )
        }
    }
}

@BindingAdapter("entries")
fun Spinner.setEntries(entries: List<Any>?) {
    if (entries != null) {
        val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, entries)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter = arrayAdapter
    }
}

@BindingAdapter("selectedValueAttrChanged")
fun Spinner.setInverseBindingListener(listener: InverseBindingListener) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            if (tag != position) {
                listener.onChange()
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
}

@BindingAdapter("selectedValue")
fun Spinner.setSelectedValue(value: Any?) {
    if (adapter != null) {
        val position = (adapter as ArrayAdapter<Any>).getPosition(value)
        setSelection(position, false)
        tag = position
    }
}

@InverseBindingAdapter(attribute = "selectedValue", event = "selectedValueAttrChanged")
fun Spinner.getSelectedValue(): Any? {
    return selectedItem
}




