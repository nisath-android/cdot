package com.naminfo.cdot_vc.activities.main.settings.viewmodels

import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences

abstract class GenericSettingsViewModel : ViewModel() {
    protected val prefs = corePreferences
    protected val core = coreContext.core
}
