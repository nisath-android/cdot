package com.naminfo.cdot_vc.activities.voip.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.utils.AppUtils
import com.naminfo.cdot_vc.utils.Event
import com.naminfo.cdot_vc.utils.LinphoneUtils
import org.linphone.core.Conference
import org.linphone.core.ConferenceListenerStub
import org.linphone.core.Participant
import org.linphone.core.ParticipantDevice
import org.linphone.core.R
import org.linphone.core.tools.Log
import kotlin.collections.orEmpty
import kotlin.text.forEach
import kotlin.text.orEmpty

class ConferenceViewModel : ViewModel() {
    val conferenceExists = MutableLiveData<Boolean>()
    val subject = MutableLiveData<String>()
    val isConferenceLocallyPaused = MutableLiveData<Boolean>()
    val isVideoConference = MutableLiveData<Boolean>()
    val isMeAdmin = MutableLiveData<Boolean>()

    val conference = MutableLiveData<Conference>()
    val conferenceCreationPending = MutableLiveData<Boolean>()
    //   val conferenceParticipants = MutableLiveData<List<ConferenceParticipantData>>()
    // val conferenceParticipantDevices = MutableLiveData<List<ConferenceParticipantDeviceData>>()
    //  val conferenceDisplayMode = MutableLiveData<ConferenceDisplayMode>()
    //   val activeSpeakerConferenceParticipantDevices = MediatorLiveData<List<ConferenceParticipantDeviceData>>()

    val isRecording = MutableLiveData<Boolean>()
    val isRemotelyRecorded = MutableLiveData<Boolean>()

    val maxParticipantsForMosaicLayout = corePreferences.maxConferenceParticipantsForMosaicLayout

    val twoOrMoreParticipants = MutableLiveData<Boolean>()
    val moreThanTwoParticipants = MutableLiveData<Boolean>()

    val speakingParticipantFound = MutableLiveData<Boolean>()

    //   val speakingParticipant = MutableLiveData<ConferenceParticipantDeviceData>()
    val speakingParticipantVideoEnabled = MutableLiveData<Boolean>()
    // val meParticipant = MutableLiveData<ConferenceParticipantDeviceData>()

    val isBroadcast = MutableLiveData<Boolean>()
    val isMeListenerOnly = MutableLiveData<Boolean>()

//    val participantAdminStatusChangedEvent: MutableLiveData<Event<ConferenceParticipantData>> by lazy {
//        MutableLiveData<Event<ConferenceParticipantData>>()
//    }

    val firstToJoinEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val allParticipantsLeftEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val secondParticipantJoinedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val moreThanTwoParticipantsJoinedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private var waitForNextStreamsRunningToUpdateLayout = false

    val reloadConferenceFragmentEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    init {
        conferenceExists.value = false
    }

    override fun onCleared() {
        super.onCleared()
    }

}