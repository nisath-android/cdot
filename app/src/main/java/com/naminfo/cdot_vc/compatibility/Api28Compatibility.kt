package com.naminfo.cdot_vc.compatibility

import android.annotation.TargetApi
import android.content.ClipboardManager

@TargetApi(28)
class Api28Compatibility {
    companion object {
        fun clearClipboard(clipboard: ClipboardManager) {
            clipboard.clearPrimaryClip()
        }
    }
}
