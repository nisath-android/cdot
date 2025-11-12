package org.naminfo.activities.main.ptt.pttrecents

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.linphone.core.RegistrationState
import org.naminfo.R

class PTTRecentsViewModel : ViewModel() {
    val registrationStatusDrawable = MutableLiveData<Int>()
    private fun getStatusIconResource(state: RegistrationState): Int {
        return when (state) {
            RegistrationState.Ok -> R.drawable.led_registered
            RegistrationState.Progress, RegistrationState.Refreshing -> R.drawable.led_registration_in_progress
            RegistrationState.Failed -> R.drawable.led_error
            else -> R.drawable.led_not_registered
        }
    }
    fun setRegistrationStatus(state: RegistrationState) {
        registrationStatusDrawable.value = getStatusIconResource(state)
    }
}
