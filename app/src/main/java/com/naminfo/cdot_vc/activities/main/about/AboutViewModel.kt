package com.naminfo.cdot_vc.activities.main.about

import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext

class AboutViewModel : ViewModel() {
    val appVersion: String = coreContext.appVersion

    val sdkVersion: String = coreContext.sdkVersion
}
