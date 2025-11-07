@file:JvmName("DataBindingUtilsKt")
package com.naminfo.cdot_vc.utils

import com.naminfo.cdot_vc.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.databinding.*

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import coil.dispose
import coil.load
import coil.request.CachePolicy
import coil.request.videoFrameMillis
import coil.transform.CircleCropTransformation
import com.google.android.material.switchmaterial.SwitchMaterial
import com.naminfo.cdot_vc.BR
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.settings.SettingListener
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModel
import com.naminfo.cdot_vc.contact.ContactAvatarGenerator
import com.naminfo.cdot_vc.contact.ContactDataInterface
import com.naminfo.cdot_vc.contact.getPictureUri
import com.naminfo.cdot_vc.views.VoiceRecordProgressBar
import kotlinx.coroutines.*

import org.linphone.core.ConsolidatedPresence
import org.linphone.core.tools.Log
import kotlin.text.toInt
import androidx.core.graphics.toColorInt


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

@BindingAdapter("layout_gravity")
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
            //binding.setVariable(BR.parent, parent)

            // This is a bit hacky...
            if (viewGroup.context is LifecycleOwner) {
                binding.lifecycleOwner = viewGroup.context as? LifecycleOwner
            }

            viewGroup.addView(binding.root)
        }
    }
}


@BindingAdapter("itemsh", "layout", requireAll = true)
fun <T> setEntriesData(viewGroup: ViewGroup, items: Any?, layoutId: Int) {
    val context = viewGroup.context
    val inflater = LayoutInflater.from(context)
    viewGroup.removeAllViews()

    val data: List<T>? = when (items) {
        is MutableLiveData<*> -> (items.value as? List<T>)
        is LiveData<*> -> (items.value as? List<T>)
        is List<*> -> (items as? List<T>)
        else -> null
    }

    if (data.isNullOrEmpty()) return

    for (item in data) {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(inflater, layoutId, viewGroup, false)
        binding.setVariable(BR.viewModel, item)
        binding.executePendingBindings()
        viewGroup.addView(binding.root)
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

@BindingAdapter("coil")
fun loadImageWithCoil(imageView: ImageView, path: String?) {
    if (!path.isNullOrEmpty() && FileUtils.isExtensionImage(path)) {
        if (corePreferences.vfsEnabled && path.startsWith(corePreferences.vfsCachePath)) {
            imageView.load(path) {
                diskCachePolicy(CachePolicy.DISABLED)
                listener(
                    onError = { _, result ->
                        Log.e(
                            "[Data Binding] [VFS] [Coil] Error loading [$path]: ${result.throwable}"
                        )
                    }
                )
            }
        } else {
            imageView.load(path) {
                listener(
                    onError = { _, result ->
                        Log.e("[Data Binding] [Coil] Error loading [$path]: ${result.throwable}")
                    }
                )
            }
        }
    } else if (path != null) {
        Log.w("[Data Binding] [Coil] Can't load $path")
    }
}



private suspend fun loadContactPictureWithCoil(
    imageView: ImageView,
    contact: ContactDataInterface?,
    useThumbnail: Boolean,
    size: Int = 0,
    textSize: Int = 0,
    color: Int = 0,
    textColor: Int = 0,
    defaultAvatar: String? = null
) {
    imageView.dispose()
    Log.d(
        "[Coil]",
        "-------"
    )
    val context = imageView.context
    if (contact == null) {
        if (defaultAvatar != null) {
            imageView.load(defaultAvatar) {
                transformations(CircleCropTransformation())
            }
        } else {
            imageView.load(R.drawable.icon_single_contact_avatar)
        }
    } else if (contact.showGroupChatAvatar) {
        imageView.load(
            AppCompatResources.getDrawable(context, R.drawable.icon_multiple_contacts_avatar)
        )
    } else {
        val displayName = contact.contact.value?.name ?: contact.displayName.value.orEmpty()
        val source = contact.contact.value?.getPictureUri(useThumbnail)
        val sourceStr = source.toString()
        Log.d(
            "[Coil]",
            "[Coil] Picture URI is base64 encoded contact.name:${contact.contact.value?.name}\n" +
                    "displayName:${contact.displayName.value.orEmpty()}\n" +
                    "sourceStr:$sourceStr"
        )
        val base64 = if (ImageUtils.isBase64(sourceStr)) {
            Log.d("[Coil] Picture URI is base64 encoded")
            ImageUtils.getBase64ImageFromString(sourceStr)
        } else {
            null
        }

        imageView.load(base64 ?: source) {
            transformations(CircleCropTransformation())
            error(
                if (displayName.isEmpty() || AppUtils.getInitials(displayName) == "+") {
                    AppCompatResources.getDrawable(context, R.drawable.icon_single_contact_avatar)
                } else {
                    coroutineScope {
                        withContext(Dispatchers.IO) {
                            val builder = ContactAvatarGenerator(context)
                            builder.setLabel(displayName)
                            if (size > 0) {
                                builder.setAvatarSize(AppUtils.getDimension(size).toInt())
                            }
                            if (textSize > 0) {
                                builder.setTextSize(AppUtils.getDimension(textSize))
                            }
                            if (color > 0) {
                                builder.setBackgroundColorAttribute(color)
                            }
                            if (textColor > 0) {
                                builder.setTextColorResource(textColor)
                            }
                            builder.build()
                        }
                    }
                }
            )
        }
    }
}

@BindingAdapter("coilContact")
fun loadContactPictureWithCoil(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoil(imageView, contact, true)
        }
    }
}
private suspend fun loadContactPictureWithCoilAlter(
    imageView: ImageView,
    contact: ContactDataInterface?,
    useThumbnail: Boolean,
    size: Int = 0,
    textSize: Int = 0,
    color: Int = 0,
    textColor: Int = 0,
    defaultAvatar: String? = null
) {
    imageView.dispose()

    val context = imageView.context
    if (contact == null) {
        if (defaultAvatar != null) {
            imageView.load(defaultAvatar) {
                transformations(CircleCropTransformation())
            }
        } else {
            imageView.load(R.drawable.icon_single_contact_avatar)
        }
    } else if (contact.showGroupChatAvatar) {
        imageView.load(
            AppCompatResources.getDrawable(context, R.drawable.icon_multiple_contacts_avatar)
        )
    } else {
        val displayName = contact.displayName.value ?: contact.contact.value?.name.orEmpty()
        val source = contact.contact.value?.getPictureUri(useThumbnail)
        val sourceStr = source.toString()
        Log.d(
            "[Coil]",
            "[Coil] Picture URI is base64 encoded contact.name:${contact.contact.value?.name}\n" +
                    "displayName:${contact.displayName.value.orEmpty()}\n" +
                    "sourceStr:$sourceStr"
        )
        val base64 = if (ImageUtils.isBase64(sourceStr)) {
            Log.d("[Coil] Picture URI is base64 encoded")
            ImageUtils.getBase64ImageFromString(sourceStr)
        } else {
            null
        }

        imageView.load(base64 ?: source) {
            transformations(CircleCropTransformation())
            error(
                if (displayName.isEmpty() || AppUtils.getInitials(displayName) == "+") {
                    AppCompatResources.getDrawable(context, R.drawable.icon_single_contact_avatar)
                } else {
                    coroutineScope {
                        withContext(Dispatchers.IO) {
                            val builder = ContactAvatarGenerator(context)
                            builder.setLabel(displayName)
                            if (size > 0) {
                                builder.setAvatarSize(AppUtils.getDimension(size).toInt())
                            }
                            if (textSize > 0) {
                                builder.setTextSize(AppUtils.getDimension(textSize))
                            }
                            if (color > 0) {
                                builder.setBackgroundColorAttribute(color)
                            }
                            if (textColor > 0) {
                                builder.setTextColorResource(textColor)
                            }
                            builder.build()
                        }
                    }
                }
            )
        }
    }
}

@BindingAdapter("coilContactAlter")
fun loadContactPictureWithCoilAlter(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoilAlter(imageView, contact, true)
        }
    }
}

@BindingAdapter("coilContactBig")
fun loadBigContactPictureWithCoil(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoil(
                imageView,
                contact,
                false,
                R.dimen.contact_avatar_big_size,
                R.dimen.contact_avatar_text_big_size
            )
        }
    }
}

@BindingAdapter("coilVoipContactAlt")
fun loadVoipContactPictureWithCoilAlt(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoil(
                imageView,
                contact,
                false,
                R.dimen.voip_contact_avatar_max_size,
                R.dimen.voip_contact_avatar_text_size,
                R.attr.voipParticipantBackgroundColor,
                R.color.white_color
            )
        }
    }
}

@BindingAdapter("coilVoipContact")
fun loadVoipContactPictureWithCoil(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoil(
                imageView,
                contact,
                false,
                R.dimen.voip_contact_avatar_max_size,
                R.dimen.voip_contact_avatar_text_size,
                R.attr.voipBackgroundColor,
                R.color.white_color
            )
        }
    }
}

@BindingAdapter("coilSelfAvatar")
fun loadSelfAvatarWithCoil(imageView: ImageView, contact: ContactDataInterface?) {
    val coroutineScope = contact?.coroutineScope ?: coreContext.coroutineScope
    coroutineScope.launch {
        withContext(Dispatchers.Main) {
            loadContactPictureWithCoil(
                imageView,
                contact,
                false,
                R.dimen.voip_contact_avatar_max_size,
                R.dimen.voip_contact_avatar_text_size,
                R.attr.voipBackgroundColor,
                R.color.white_color,
                corePreferences.defaultAccountAvatarPath
            )
        }
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

@BindingAdapter("assistantPhoneNumberValidation")
fun addPhoneNumberEditTextValidation(editText: EditText, enabled: Boolean) {
    if (!enabled) return
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            when {
                s?.matches(Regex("\\d+")) == false ->
                    editText.error =
                        editText.context.getString(
                            R.string.assistant_error_phone_number_invalid_characters
                        )
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter("assistantPhoneNumberPrefixValidation")
fun addPrefixEditTextValidation(editText: EditText, enabled: Boolean) {
    if (!enabled) return
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if ((s?.length ?: 0) > 1) {
                val dialPlan = PhoneNumberUtils.getDialPlanFromCountryCallingPrefix(
                    s.toString().substring(1)
                )
                if (dialPlan == null) {
                    editText.error =
                        editText.context.getString(
                            R.string.assistant_error_invalid_international_prefix
                        )
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        @SuppressLint("SetTextI18n")
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty() || !s.startsWith("+")) {
                editText.setText("+$s")
            }
        }
    })
}

@BindingAdapter("assistantUsernameValidation")
fun addUsernameEditTextValidation(editText: EditText, enabled: Boolean) {
    if (!enabled) return
    val usernameRegexp = corePreferences.config.getString(
        "assistant",
        "username_regex",
        "^[a-z0-9+_.\\-]*\$"
    )!!
    val usernameMaxLength = corePreferences.config.getInt("assistant", "username_max_length", 64)
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            when {
                s?.matches(Regex(usernameRegexp)) == false ->
                    editText.error =
                        editText.context.getString(
                            R.string.assistant_error_username_invalid_characters
                        )
                (s?.length ?: 0) > usernameMaxLength -> {
                    editText.error =
                        editText.context.getString(R.string.assistant_error_username_too_long)
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

@BindingAdapter("emailConfirmationValidation")
fun addEmailEditTextValidation(editText: EditText, enabled: Boolean) {
    if (!enabled) return
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                editText.error =
                    editText.context.getString(R.string.assistant_error_invalid_email_address)
            }
        }
    })
}

@BindingAdapter("urlConfirmationValidation")
fun addUrlEditTextValidation(editText: EditText, enabled: Boolean) {
    if (!enabled) return
    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (!Patterns.WEB_URL.matcher(s).matches()) {
                editText.error =
                    editText.context.getString(R.string.assistant_remote_provisioning_wrong_format)
            }
        }
    })
}

@BindingAdapter("passwordConfirmationValidation")
fun addPasswordConfirmationEditTextValidation(password: EditText, passwordConfirmation: EditText) {
    password.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (passwordConfirmation.text == null || s == null || passwordConfirmation.text.toString() != s.toString()) {
                passwordConfirmation.error =
                    passwordConfirmation.context.getString(
                        R.string.assistant_error_passwords_dont_match
                    )
            } else {
                passwordConfirmation.error = null // To clear other edit text field error
            }
        }
    })

    passwordConfirmation.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (password.text == null || s == null || password.text.toString() != s.toString()) {
                passwordConfirmation.error =
                    passwordConfirmation.context.getString(
                        R.string.assistant_error_passwords_dont_match
                    )
            }
        }
    })
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

@BindingAdapter("max")
fun VoiceRecordProgressBar.setProgressMax(max: Int) {
    setMax(max)
}

@BindingAdapter("android:progress")
fun VoiceRecordProgressBar.setPrimaryProgress(progress: Int) {
    setProgress(progress)
}

@BindingAdapter("android:secondaryProgress")
fun VoiceRecordProgressBar.setSecProgress(progress: Int) {
    setSecondaryProgress(progress)
}

@BindingAdapter("secondaryProgressTint")
fun VoiceRecordProgressBar.setSecProgressTint(color: Int) {
    setSecondaryProgressTint(color)
}

@BindingAdapter("android:layout_margin")
fun setConstraintLayoutMargins(view: View, margins: Float) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    val m = margins.toInt()
    params.setMargins(m, m, m, m)
    view.layoutParams = params
}

@BindingAdapter("android:layout_marginTop")
fun setConstraintLayoutTopMargin(view: View, margins: Float) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    val m = margins.toInt()
    params.setMargins(params.leftMargin, m, params.rightMargin, params.bottomMargin)
    view.layoutParams = params
}


@BindingAdapter("android:layout_marginBottom")
fun setConstraintLayoutBottomMargin(view: View, margins: Float) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    val m = margins.toInt()
    params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, m)
    view.layoutParams = params
}

@BindingAdapter("android:layout_marginEnd")
fun setConstraintLayoutEndMargin(view: View, margins: Float) {
    val params = view.layoutParams as ConstraintLayout.LayoutParams
    val m = margins.toInt()
    params.marginEnd = m
    view.layoutParams = params
}

@BindingAdapter("android:onTouch")
fun View.setTouchListener(listener: View.OnTouchListener?) {
    if (listener != null) {
        setOnTouchListener(listener)
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



@BindingAdapter("presenceIcon")
fun ImageView.setPresenceIcon(presence: ConsolidatedPresence?) {
    if (presence == null) return

    val icon = when (presence) {
        ConsolidatedPresence.Online -> R.drawable.led_online
        ConsolidatedPresence.DoNotDisturb -> R.drawable.led_do_not_disturb
        ConsolidatedPresence.Busy -> R.drawable.led_away
        else -> R.drawable.led_not_registered
    }
    setImageResource(icon)

    val contentDescription = when (presence) {
        ConsolidatedPresence.Online -> AppUtils.getString(
            R.string.content_description_presence_online
        )
        ConsolidatedPresence.DoNotDisturb -> AppUtils.getString(
            R.string.content_description_presence_do_not_disturb
        )
        else -> AppUtils.getString(R.string.content_description_presence_offline)
    }
    setContentDescription(contentDescription)
}

/*@BindingAdapter("customCardBackground")
fun setCustomCardBackground(cardView: CardView, colorString: String?) {
    val color = try {
        colorString?.toColorInt() ?: Color.parseColor("#E3F2FD")
    } catch (e: Exception) {
        Color.parseColor("#E3F2FD")
    }
    cardView.setCardBackgroundColor(color)
}*/

/*@BindingAdapter("customCardBackground")
fun setCustomCardBackground(cardView: CardView, colorString: String?) {
    android.util.Log.d("TAG", "setCustomCardBackground: ")

    val colors = listOf(
        Color.rgb(255, 204, 153), // #ffcc99
        Color.rgb(204, 204, 255), // #ccccff
        Color.rgb(153, 255, 153), // #99ff99
        Color.rgb(255, 153, 153)  // #ff9999
    )

    val random = java.util.Random()
    val randomLightFogColor = colors[random.nextInt(colors.size)]

    val color = try {
        if (!colorString.isNullOrEmpty()) {
            colorString.toColorInt()
        } else {
            randomLightFogColor
        }
    } catch (e: Exception) {
        randomLightFogColor
    }

    cardView.setCardBackgroundColor(color)
}*/
@BindingAdapter("titleTextColor")
fun setTitleTextColor(view: TextView, color: Int) {
    view.setTextColor(color)
}
@BindingAdapter("titleTextSizes")
fun setTitleTextSize(view: TextView, size: Float) {
    view.setTextSize(size)
}
@BindingAdapter("customCardBackground")
fun setCustomCardBackground(cardView: CardView, colorString: String?) {
    android.util.Log.d("TAG", "setCustomCardBackground: ")
    val random = java.util.Random()
    val randomLightFogColor = Color.rgb(
        180 + random.nextInt(50),  // R: 180–230
        200 + random.nextInt(55),  // G: 200–255
        220 + random.nextInt(35)   // B: 220–255 (more blue)
    )


    val color = try {
        if (!colorString.isNullOrEmpty()) {
            // Use given color if available, else random
            colorString.toColorInt()
        } else {
            randomLightFogColor
        }
    } catch (e: Exception) {
        randomLightFogColor
    }

    cardView.setCardBackgroundColor(color)
}


/*@BindingAdapter("customCardBackground")
fun setCustomCardBackground(cardView: CardView, colorString: String?) {
    val context = cardView.context
    android.util.Log.d("TAG", "setCustomCardBackground: Brand Gradient")

    // Brand palette
    val primary = ContextCompat.getColor(context, R.color.primary_color)
    val dark = ContextCompat.getColor(context, R.color.primary_dark_color)
    val light = ContextCompat.getColor(context, R.color.primary_light_color)

    // Optionally, slightly brighten/darken for depth
    fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * factor).coerceAtMost(255f)).toInt()
        val g = ((Color.green(color) * factor).coerceAtMost(255f)).toInt()
        val b = ((Color.blue(color) * factor).coerceAtMost(255f)).toInt()
        return Color.rgb(r, g, b)
    }

    val startColor = colorString?.toColorInt() ?: light
    val centerColor = adjustColorBrightness(primary, 0.9f)
    val endColor = adjustColorBrightness(dark, 0.8f)

    // Luxurious 3-stop gradient: top-left → bottom-right
    val gradient = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(startColor, centerColor, endColor)
    ).apply {
        cornerRadius = cardView.radius
        gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    // Use gradient as the visible layer
    cardView.setCardBackgroundColor(Color.TRANSPARENT)
    cardView.background = gradient

    // Optional subtle elevation polish (makes gradient pop)
   // cardView.cardElevation = 8f
}*/







@BindingAdapter("customCardBackground")
fun setCustomCardBackground(cardView: CardView, colorLiveData: LiveData<String>?) {
    val colorString = colorLiveData?.value
    try {
        if (!colorString.isNullOrEmpty()) {
            cardView.setCardBackgroundColor(colorString.toColorInt())
        } else {
            cardView.setCardBackgroundColor("#ff0000".toColorInt())
        }
    } catch (e: Exception) {
        cardView.setCardBackgroundColor(Color.parseColor("#ff0000"))
    }
}





