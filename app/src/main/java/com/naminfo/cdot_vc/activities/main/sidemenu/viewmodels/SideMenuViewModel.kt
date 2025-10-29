package com.naminfo.cdot_vc.activities.main.sidemenu.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.activities.main.settings.SettingListenerStub
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.AccountSettingsViewModel
import org.linphone.core.*
import com.naminfo.cdot_vc.utils.LinphoneUtils

class SideMenuViewModel : ViewModel() {
    val showAccounts: Boolean = corePreferences.showAccountsInSideMenu
    val showAssistant: Boolean = corePreferences.showAssistantInSideMenu
    val showSettings: Boolean = corePreferences.showSettingsInSideMenu
    val showRecordings: Boolean = corePreferences.showRecordingsInSideMenu
    val showScheduledConferences = MutableLiveData<Boolean>()
    val updated = MutableLiveData<String>()
    val showAbout: Boolean = corePreferences.showAboutInSideMenu
    val showQuit: Boolean = corePreferences.showQuitInSideMenu

    val defaultAccountViewModel = MutableLiveData<AccountSettingsViewModel>()
    val defaultAccountFound = MutableLiveData<Boolean>()
    val defaultAccountAvatar = MutableLiveData<String>()

    val accounts = MutableLiveData<ArrayList<AccountSettingsViewModel>>()

    val presenceStatus = MutableLiveData<ConsolidatedPresence>()

    lateinit var accountsSettingsListener: SettingListenerStub

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            // +1 is for the default account, otherwise this will trigger every time
            if (accounts.value.isNullOrEmpty() ||
                coreContext.core.accountList.size != accounts.value.orEmpty().size + 1
            ) {
                // Only refresh the list if an account has been added or removed
                Log.i("[Assistant] [Generic Login]", "onAccountRegistrationStateChanged")
                updateAccountsList()
            }
        }
    }

    init {
        defaultAccountFound.value = false
        defaultAccountAvatar.value = corePreferences.defaultAccountAvatarPath
        showScheduledConferences.value = corePreferences.showScheduledConferencesInSideMenu &&
            LinphoneUtils.isRemoteConferencingAvailable()
        coreContext.core.addListener(listener)
        Log.i("[Assistant] [Generic Login]", "Init")
        updateAccountsList()
        refreshConsolidatedPresence()
    }

    override fun onCleared() {
        defaultAccountViewModel.value?.destroy()
        accounts.value.orEmpty().forEach(AccountSettingsViewModel::destroy)
        coreContext.core.removeListener(listener)
        super.onCleared()
    }

    fun refreshConsolidatedPresence() {
        presenceStatus.value = coreContext.core.consolidatedPresence
    }

    fun updateAccountsList() {
        defaultAccountFound.value = false // Do not assume a default account will still be found
        defaultAccountViewModel.value?.destroy()
        accounts.value.orEmpty().forEach(AccountSettingsViewModel::destroy)

        val list = arrayListOf<AccountSettingsViewModel>()
        val defaultAccount = coreContext.core.defaultAccount
        if (defaultAccount != null) {
            val defaultViewModel = AccountSettingsViewModel(defaultAccount)
            defaultViewModel.accountsSettingsListener = object : SettingListenerStub() {
                override fun onAccountClicked(identity: String) {
                    accountsSettingsListener.onAccountClicked(identity)
                }
            }
            corePreferences.getCurrentUserPhoneNumber = defaultViewModel.userName.value ?: defaultViewModel.displayName.value
            corePreferences.getCurrentUserName = defaultViewModel.displayName.value ?: defaultViewModel.userName.value

            Log.i(
                "[Assistant] [Generic Login]",
                "Sidemenu-7-accounts=${defaultViewModel.userName.value} | ${defaultViewModel.displayName.value}"
            )
            updated.value = defaultViewModel.displayName.value
            defaultAccountViewModel.value = defaultViewModel
            defaultAccountFound.value = true
        }

        for (account in LinphoneUtils.getAccountsNotHidden()) {
            if (account != coreContext.core.defaultAccount) {
                val viewModel = AccountSettingsViewModel(account)
                viewModel.accountsSettingsListener = object : SettingListenerStub() {
                    override fun onAccountClicked(identity: String) {
                        accountsSettingsListener.onAccountClicked(identity)
                    }
                }
                list.add(viewModel)
            }
        }
        accounts.value = list

        showScheduledConferences.value = corePreferences.showScheduledConferencesInSideMenu &&
            LinphoneUtils.isRemoteConferencingAvailable()
    }

    fun setPictureFromPath(picturePath: String) {
        corePreferences.defaultAccountAvatarPath = picturePath
        defaultAccountAvatar.value = corePreferences.defaultAccountAvatarPath
        coreContext.contactsManager.updateLocalContacts()
    }
}
