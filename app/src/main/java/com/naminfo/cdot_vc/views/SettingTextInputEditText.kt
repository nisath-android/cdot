package com.naminfo.cdot_vc.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import com.google.android.material.textfield.TextInputEditText
import com.naminfo.cdot_vc.activities.main.settings.SettingListener

//import org.linphone.activities.main.settings.SettingListener

class SettingTextInputEditText : TextInputEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun fakeImeDone(listener: SettingListener) {
        listener.onTextValueChanged(text.toString())

        // Send IME action DONE to trigger onSettingImeDone binding adapter, but that doesn't work...
        // val inputConnection = BaseInputConnection(this, true)
        // inputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE)

        // Will make check icon to disappear thanks to onFocusChangeVisibilityOf binding adapter
        clearFocus()

        // Hide keyboard
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}
