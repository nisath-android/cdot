package com.naminfo.cdot_vcactivities.assistant.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import java.util.*
import org.linphone.core.*
import org.linphone.core.tools.Log

class SharedAssistantViewModel : ViewModel() {
    val remoteProvisioningUrl = MutableLiveData<String>()

    private var accountCreator: AccountCreator
    private var useGenericSipAccount: Boolean = false

    init {
        Log.i("[Assistant] Loading linphone default values useGenericSipAccount - $useGenericSipAccount")
        coreContext.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
        accountCreator = coreContext.core.createAccountCreator(corePreferences.xmlRpcServerUrl)
        accountCreator.language = Locale.getDefault().language
    }

    fun getAccountCreator(genericAccountCreator: Boolean = false): AccountCreator {
        if (genericAccountCreator != useGenericSipAccount) {
            accountCreator.reset()
            accountCreator.language = Locale.getDefault().language

            if (genericAccountCreator) {
                Log.i("[Assistant] Loading default values")
                coreContext.core.loadConfigFromXml(corePreferences.defaultValuesPath)
            } else {
                Log.i("[Assistant] Loading linphone default values")
                coreContext.core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)
            }
            useGenericSipAccount = genericAccountCreator
        }
        return accountCreator
    }
}
