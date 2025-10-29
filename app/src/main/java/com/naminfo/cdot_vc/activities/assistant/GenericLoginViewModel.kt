package com.naminfo.cdot_vc.activities.assistant.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.utils.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.Account
import org.linphone.core.AccountCreator
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import org.linphone.core.tools.Log

class GenericLoginViewModelFactory(private val accountCreator: AccountCreator) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GenericLoginViewModel(accountCreator) as T
    }
}

class GenericLoginViewModel(private val accountCreator: AccountCreator) : ViewModel() {

    //private lateinit var data: ContactEditorData
    val username = MutableLiveData<String>()

    val password = MutableLiveData<String>()

    val domain = MutableLiveData<String>().apply {
        value = "192.168.1.50"
        // value = "103.16.202.169"
    }

    val displayName = MutableLiveData<String>()

    val transport = MutableLiveData<TransportType>()

    val loginEnabled: MediatorLiveData<Boolean> = MediatorLiveData()

    val waitForServerAnswer = MutableLiveData<Boolean>()

    val leaveAssistantEvent = MutableLiveData<Event<Boolean>>()

    val invalidCredentialsEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val onErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private var accountToCheck: Account? = null

    // MutableLiveData to observe the CheckBox state
    val isChecked = MutableLiveData(false) // Initialize with default value false

    // Function to toggle the checkbox state
    fun toggleCheckbox() {
        isChecked.value = isChecked.value?.not() // Toggle the current value
    }

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            // if (state?.name=="Ok")
            Log.i(
                "[Assistant] [Generic Login] ----------Registration state is $state: $message-----------"
            )
            if (account == accountToCheck) {
                Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    waitForServerAnswer.value = false
                    leaveAssistantEvent.value = Event(true)
                    core.removeListener(this)
                    Log.i(
                        "[Assistant] [Generic Login] Domain ${account.core.authInfoList.get(0).domain}"
                    )
                    // saveDomainToSharedPrefs(, account.core.authInfoList.get(0).domain)
                } else if (state == RegistrationState.Failed) {
                    waitForServerAnswer.value = false
                    invalidCredentialsEvent.value = Event(true)
                    core.removeListener(this)
                }
            }
        }
    }

    init {
        transport.value = TransportType.Udp

        loginEnabled.value = false
        loginEnabled.addSource(username) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(password) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(domain) {
            loginEnabled.value = isLoginButtonEnabled()
        }
    }

    fun setTransport(transportType: TransportType) {
        transport.value = transportType
    }

    fun removeInvalidProxyConfig() {
        val account = accountToCheck
        account ?: return

        val core = coreContext.core
        val authInfo = account.findAuthInfo()
        if (authInfo != null) core.removeAuthInfo(authInfo)
        core.removeAccount(account)
        accountToCheck = null

        // Make sure there is a valid default account
        val accounts = core.accountList
        if (accounts.isNotEmpty() && core.defaultAccount == null) {
            core.defaultAccount = accounts.first()
            core.refreshRegisters()
        }
    }

    fun continueEvenIfInvalidCredentials() {
        leaveAssistantEvent.value = Event(true)
    }

    fun createAccountAndAuthInfo() {
        coreContext.core.addListener(coreListener)
        Log.i("[Assistant] [Generic Login] Username - ${username.value}")
        if (username.value.isNullOrEmpty()) {
            waitForServerAnswer.value = false
            onErrorEvent.value = Event("Empty username not allowed")
            Log.i("[Assistant] [Generic Login] Username empty")
        } else {
            if (password.value.isNullOrEmpty()) {
                waitForServerAnswer.value = false
                onErrorEvent.value = Event("Empty password not allowed")
            } else if (domain.value.isNullOrEmpty()) {
                waitForServerAnswer.value = false
                onErrorEvent.value = Event("Empty domain not allowed")
            } else {
                waitForServerAnswer.value = true
                val domain = domain.value
                val loginDetails: ArrayList<Array<String>> = ArrayList()

                loginDetails.add(arrayOf("UserName", username.value.toString()))
                loginDetails.add(arrayOf("Password", password.value.toString()))

                // Launch a coroutine to handle the authentication asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    // val isAuthenticated = authenticate(loginDetails, domain)
                    withContext(Dispatchers.Main) {
                        /*if (isAuthenticated) {

                        } else {
                            Log.e("[Assistant] [Generic Login]", "Authentication failed.")
                            onErrorEvent.value = Event("Error: Authentication failed")
                        }*/
                        Log.i("[Assistant] [Generic Login]", " Authentication successful!")

                        // Only proceed to create the account if authenticated
                        accountCreator.username = username.value
                        accountCreator.password = password.value
                        accountCreator.domain = domain
                        accountCreator.displayName = displayName.value
                        accountCreator.transport = transport.value
                        corePreferences.getCurrentUserPhoneNumber = username.value
                        corePreferences.getCurrentUserName = displayName.value ?: username.value
                        Log.i(
                            "[Assistant] [Generic Login]",
                            "Authentication successful!-${displayName.value} | ${username.value}"
                        )

                        val account = accountCreator.createAccountInCore()
                        accountToCheck = account
                        Log.i(
                            "[Assistant] [Generic Login]",
                            "Authentication registeration state!-${account?.state} | ${username.value}"
                        )
                        if (account == null) {
                            Log.e(
                                "[Assistant] [Generic Login] Account creator couldn't create account"
                            )
                            coreContext.core.removeListener(coreListener)
                            onErrorEvent.value = Event("Error: Failed to create account object")
                        } else {
                            Log.i(
                                "[Assistant] [Generic Login] Account successfully created for domain: $domain"
                            )
                           /* coreContext.core.videoActivationPolicy.automaticallyInitiate =
                                true // Enable video initiation
                            coreContext.core.videoActivationPolicy.automaticallyAccept =
                                true // Enable video acceptance
                            coreContext.core.isVideoCaptureEnabled = true // Enable video capture
                            coreContext.core.isVideoDisplayEnabled =
                                true // Ensure video display is enabled*/
                            Log.i(
                                "[Main Activity] video enable after enabling:  ${coreContext.core.isVideoEnabled}"
                            )
                        }
                        waitForServerAnswer.value = false
                    }
                }
            }
        }
    }

     private fun isLoginButtonEnabled(): Boolean {
        return username.value.orEmpty().isNotEmpty() &&
            domain.value.orEmpty().isNotEmpty() &&
            password.value.orEmpty().isNotEmpty()
    }

}

