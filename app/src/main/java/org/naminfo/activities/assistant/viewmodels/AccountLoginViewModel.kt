package org.naminfo.activities.assistant.viewmodels

import android.os.StrictMode
import androidx.lifecycle.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.utils.Event

class AccountLoginViewModelFactory(private val accountCreator: AccountCreator) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountLoginViewModel(accountCreator) as T
    }
}

class AccountLoginViewModel(accountCreator: AccountCreator) : AbstractPhoneViewModel(accountCreator) {
    private val httpTimeout = 3000
    val loginWithUsernamePassword = MutableLiveData<Boolean>()

    val username = MutableLiveData<String>()
    val usernameError = MutableLiveData<String>()

    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<String>()

    private val _loginEvent = MutableLiveData<Event<Boolean>>()
    val loginEvent: LiveData<Event<Boolean>> get() = _loginEvent

    private val _loginResult = MutableLiveData<String>()
    val loginResult: LiveData<String> get() = _loginResult

    private val mobionLoginUrl = "http://mobionglobal.com/webservices/Mobion.asmx/Login"

    val onErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private var accountToCheck: Account? = null

    override fun onCleared() {
        super.onCleared()
    }

    override fun onFlexiApiTokenReceived() {
        Log.i("[Assistant] [Account Login] Using FlexiAPI auth token [${accountCreator.token}]")
    }

    override fun onFlexiApiTokenRequestError() {
        Log.e("[Assistant] [Account Login] Failed to get an auth token from FlexiAPI")
        onErrorEvent.value = Event("Error: Failed to get an auth token from account manager server")
    }

    fun login() {
        val un = username.value
        val pw = password.value
        loginWithUsername(un.toString(), pw.toString())
        Log.i("AccountLoginViewmodel", "UserName : $un")
        Log.i("AccountLoginViewmodel", "password : $pw")
    }

    private fun loginWithUsername(username: String, password: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                loginAuthenticationSigninClicked(
                    username,
                    password,
                    Date().toGMTString(),
                    "India",
                    "91"
                )
            }
            _loginResult.value = result
            if (result == "success") {
                corePreferences.getCurrentUserPhoneNumber = username
                _loginEvent.value = Event(true) // Post login success event
            } else {
                _loginEvent.value = Event(false) // Post login failure event if needed
            }
        }
    }

    private fun loginAuthenticationSigninClicked(
        username: String,
        password: String,
        gmtTime: String,
        country: String,
        countryCode: String
    ): String {
        threadPolicy()
        val params = ArrayList<NameValuePair>(10)
        val ipAddress = ""
        val refId = ""
        val imsi = ""
        val imei = ""
        val networkType = ""
        params.add(BasicNameValuePair("subscriberid", username))
        params.add(BasicNameValuePair("subscriberpassword", password))
        params.add(BasicNameValuePair("subscriberip", ipAddress))
        params.add(BasicNameValuePair("loginrefid", refId))
        params.add(BasicNameValuePair("gmttime", gmtTime))
        params.add(BasicNameValuePair("imsino", imsi))
        params.add(BasicNameValuePair("imeino", imei))
        params.add(BasicNameValuePair("networktype", networkType))
        params.add(BasicNameValuePair("CurrentCountry", country))
        params.add(BasicNameValuePair("CurrentCountrycode", countryCode))

        return try {
            val responseLogin = executeHttpPost(mobionLoginUrl, params)
            var res = responseLogin.trim()
            Log.i("AccountLoginViewmodel", "webservice ok : $res")

            res = res.substring(res.indexOf("<status>") + 8, res.lastIndexOf("</status>")).toLowerCase(
                Locale.ROOT
            )
            if (res == "success") {
                params.clear()
                "success"
            } else {
                var message = responseLogin.trim()
                message = responseLogin.substring(
                    message.indexOf("<message>") + 9,
                    message.lastIndexOf("</message>")
                )
                params.clear()
                message
            }
        } catch (e: Exception) {
            e.printStackTrace()
            params.clear()
            "Please register after sometime..."
        }
    }

    private fun executeHttpPost(url: String, postParameters: ArrayList<NameValuePair>): String {
        val nl = System.getProperty("line.separator")
        var inBufferedReader: BufferedReader? = null
        return try {
            val request = HttpPost(url)
            request.entity = UrlEncodedFormEntity(postParameters)
            val response = getHttpClient()?.execute(request)
            inBufferedReader = BufferedReader(InputStreamReader(response?.entity?.content))
            val sb = StringBuffer()
            while (true) {
                val line = inBufferedReader.readLine() ?: break
                sb.append(line + nl)
            }
            sb.toString()
        } finally {
            inBufferedReader?.close()
        }
    }
    private var mHttpClient: HttpClient? = null
    private fun getHttpClient(): HttpClient? {
        try {
            if (mHttpClient == null) {
                val params1 = BasicHttpParams()
                val schemeRegistry = SchemeRegistry()
                schemeRegistry.register(Scheme("http", PlainSocketFactory.getSocketFactory(), 80))
                val sslSocketFactory: SSLSocketFactory = SSLSocketFactory.getSocketFactory()
                schemeRegistry.register(Scheme("https", sslSocketFactory, 443))
                val cm: ClientConnectionManager =
                    ThreadSafeClientConnManager(params1, schemeRegistry)
                mHttpClient = DefaultHttpClient(cm, params1)
                val params: HttpParams = (mHttpClient as DefaultHttpClient).getParams()
                HttpConnectionParams.setConnectionTimeout(params, httpTimeout)
                HttpConnectionParams.setSoTimeout(params, httpTimeout)
                ConnManagerParams.setTimeout(params, httpTimeout.toLong())
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return mHttpClient
    }

    private fun threadPolicy() {
        try {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
