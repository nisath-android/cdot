package com.naminfo.cdot_vc.activities

import androidx.annotation.StringRes

interface SnackBarActivity {
    fun showSnackBar(@StringRes resourceId: Int)
    fun showSnackBar(@StringRes resourceId: Int, action: Int, listener: () -> Unit)
    fun showSnackBar(message: String)
}
