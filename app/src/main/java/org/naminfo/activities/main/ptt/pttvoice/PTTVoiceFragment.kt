package org.naminfo.activities.main.ptt.pttvoice

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.naminfo.LinphoneApplication
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.activities.main.ptt.PTTFragment
import org.naminfo.activities.main.ptt.pttrecents.PTTRecentsFragment
import org.naminfo.databinding.FragmentPttVoiceBinding

class PTTVoiceFragment : SecureFragment<FragmentPttVoiceBinding>() {
    val handler: Handler = Handler(Looper.getMainLooper())
    companion object {
        private const val ARG_USER_NAME = "arg_user_name"
        private const val ARG_PHONE = "arg_phone"
        private const val ARG_AVATAR = "arg_avatar"

        fun newInstance(name: String, phone: String, avatar: String): PTTVoiceFragment {
            return PTTVoiceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_NAME, name)
                    putString(ARG_PHONE, phone)
                    putString(ARG_AVATAR, avatar)
                }
            }
        }
    }
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var cameraImageUri: Uri? = null

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var userName: String
    private lateinit var userPhone: String
    private var audioFile: File? = null
    private val viewModel: PTTVoiceViewModel by viewModels()
    override fun getLayoutId(): Int {
        return R.layout.fragment_ptt_voice
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    cameraImageUri?.let {
                        // Use image (e.g., display or upload)
                        Log.d("Camera", "Picture saved at: $it")
                    }
                }
            }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userName = arguments?.getString(ARG_USER_NAME).toString()
        userPhone = arguments?.getString(ARG_PHONE).toString()
        val avatar = arguments?.getString(ARG_AVATAR)
        Log.i("PTT Voice", "UserName : $userName")
        binding.tvName.text = userName
        binding.lifecycleOwner = viewLifecycleOwner

        binding.imgRecentVoice.setOnClickListener {
            audioFile?.let { file ->
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(file.absolutePath)
                mediaPlayer.prepare()
                mediaPlayer.start()

                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
            } ?: run {
                Toast.makeText(requireContext(), "No recording found", Toast.LENGTH_SHORT).show()
            }
        }

        setBackIcon()
        setPTTMic()
        setupAudioToggle()
        setupVolumeUI()
        showBackPress()
        setPTTCamera()
    }

    private fun setPTTCamera() {
    }

    private fun takePhotoFromCamera() {
        val photoFile = File.createTempFile("PTT_", ".jpg", requireContext().cacheDir)
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri)
    }
    private fun setBackIcon() {
        binding.btnBack.setOnClickListener { loadFragment(PTTRecentsFragment()) }
    }

    private var isSpeakerOn = true

    private fun setupAudioToggle() {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.btnToggleAudio.setOnClickListener {
            isSpeakerOn = !isSpeakerOn

            audioManager.isSpeakerphoneOn = isSpeakerOn
            if (!isSpeakerOn) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
            } else {
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
            }

            updateButtonUI()
        }
    }

    private fun updateButtonUI() {
        binding.btnToggleAudio.text = if (isSpeakerOn) "Speaker" else "Earpiece"
        binding.btnToggleAudio.setIconResource(
            if (isSpeakerOn) R.drawable.icon_speaker else R.drawable.icon_earpiece
        )
    }
    private fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }
    private val listener = LinphoneApplication.coreContext.core.addListener(
        object : CoreListenerStub() {

            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State,
                message: String
            ) {
                org.linphone.core.tools.Log.i("[Context] -----Call state changed [$state]")
                val sdp = call.currentParams.getRtpProfile().toString()
                org.linphone.core.tools.Log.i("Linphone", "---Generated SDP: $sdp")
                if (state == Call.State.IncomingReceived || state == Call.State.IncomingEarlyMedia) {
                    if (corePreferences.autoAnswerEnabled) {
                        coreContext.answerCall(call)
                    }
                } else if (state == Call.State.OutgoingProgress) {
                } else {
                    if (state == Call.State.Connected) {
                    } else if (state == Call.State.StreamsRunning) {
                    } else if (state == Call.State.End || state == Call.State.Error || state == Call.State.Released) {
                    }
                }
            }

            override fun onLastCallEnded(core: Core) {
                org.linphone.core.tools.Log.i("[Context] Last call has ended")
                // removeCallOverlay()
                if (!core.isMicEnabled) {
                    org.linphone.core.tools.Log.w(
                        "[Context] Mic was muted in Core, enabling it back for next call"
                    )
                    core.isMicEnabled = true
                }
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    private fun setPTTMic() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.ptt_active_mic)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.ptt_inactive_mic)
        val defaultCardColor = ContextCompat.getColor(requireContext(), R.color.white_color)

        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startWaveAnimation(binding.waveCircle)
                    // Change outer ring (background tint or animator)
                    animateOuterRing(binding.outerRing, inactiveColor, activeColor)
                    // Change mic color
                    binding.imgMic.setColorFilter(activeColor)
                    // Change card stroke
                    binding.pttRecordMicCard.setStrokeColor(activeColor)
                    LinphoneApplication.corePreferences.isThisPTTCall = true
                    coreContext.core.videoActivationPolicy.automaticallyInitiate = false // Disable video initiation
                    coreContext.core.videoActivationPolicy.automaticallyAccept = false // Disable video acceptance
                    coreContext.core.isVideoCaptureEnabled = false // Ensure video is disabled
                    coreContext.core.isVideoDisplayEnabled = false
                    coreContext.core.currentCall?.currentParams?.isVideoEnabled = false
                    updateVideoActivationPolicy(true)
                    LinphoneApplication.corePreferences.isThisPTTCall = true
                    LinphoneApplication.corePreferences.autoAnswerEnabled = true

                    LinphoneApplication.coreContext.startPTTCall(userPhone.toString())
                    startRecording()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    LinphoneApplication.corePreferences.isThisPTTCall = false
                    LinphoneApplication.corePreferences.autoAnswerEnabled = false

                    stopWaveAnimation(binding.waveCircle)
                    animateOuterRing(binding.outerRing, activeColor, inactiveColor)

                    binding.pttRecordMicCard.setStrokeColor(defaultCardColor)
                    // Change mic color
                    binding.imgMic.setColorFilter(inactiveColor)
                    // Optional: simulate click for accessibility
                    v.performClick()
                    stopRecording()
                    true
                }

                else -> false
            }
        }

        // Apply the same listener to both
        binding.pttRecordMicCard.setOnTouchListener(touchListener)
        binding.outerRing.setOnTouchListener(touchListener)
    }

    private fun startRecording() {
        audioFile = File(requireContext().cacheDir, "ptt_${System.currentTimeMillis()}.mp4")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(audioFile!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun animateOuterRing(view: View, startColor: Int, endColor: Int) {
        val animator = ValueAnimator.ofArgb(startColor, endColor)
        animator.duration = 300 // duration in ms
        animator.addUpdateListener {
            view.background.setTint(it.animatedValue as Int)
        }
        animator.start()
    }

    private fun startWaveAnimation(waveView: View) {
        waveView.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(waveView, "scaleX", 1f, 2.5f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        val scaleY = ObjectAnimator.ofFloat(waveView, "scaleY", 1f, 2.5f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        val alpha = ObjectAnimator.ofFloat(waveView, "alpha", 1f, 0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopWaveAnimation(waveView: View) {
        waveView.animate().cancel()
        waveView.clearAnimation()
        waveView.visibility = View.GONE
        waveView.scaleX = 1f
        waveView.scaleY = 1f
        waveView.alpha = 1f
    }

    private fun highlightIcon(up: Boolean) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.ptt_active_mic)
        val dimColor = ContextCompat.getColor(requireContext(), R.color.ptt_inactive_mic)

        if (up) {
            binding.btnVolumeUp.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            binding.btnVolumeDown.setColorFilter(dimColor, PorterDuff.Mode.SRC_IN)
        } else {
            binding.btnVolumeDown.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
            binding.btnVolumeUp.setColorFilter(dimColor, PorterDuff.Mode.SRC_IN)
        }
    }
    private fun setupVolumeUI() {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.volumeSeekbar.max = maxVolume
        binding.volumeSeekbar.progress = currentVolume

        updateVolumeIconState(currentVolume, maxVolume)

        binding.volumeSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentVolume = progress
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    updateVolumeIconState(currentVolume, maxVolume)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnVolumeUp.setOnClickListener {
            if (currentVolume < maxVolume) {
                currentVolume++
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                binding.volumeSeekbar.progress = currentVolume
                highlightIcon(up = true)
            }
        }

        binding.btnVolumeDown.setOnClickListener {
            if (currentVolume > 0) {
                currentVolume--
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                binding.volumeSeekbar.progress = currentVolume
                highlightIcon(up = false)
            }
        }
    }

    private fun updateVolumeIconState(currentVolume: Int, maxVolume: Int) {
        val color = ContextCompat.getColor(requireContext(), R.color.ptt_active_mic)
        val dimColor = ContextCompat.getColor(requireContext(), R.color.ptt_inactive_mic)

        if (currentVolume == 0) {
            binding.btnVolumeDown.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.btnVolumeUp.setColorFilter(dimColor, PorterDuff.Mode.SRC_IN)
        } else if (currentVolume == maxVolume) {
            binding.btnVolumeDown.setColorFilter(dimColor, PorterDuff.Mode.SRC_IN)
            binding.btnVolumeUp.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else {
            binding.btnVolumeDown.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.btnVolumeUp.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val maybePTTParent = parentFragment

        if (maybePTTParent is PTTFragment) {
            // Use custom fragment transaction inside PTTFragment
            maybePTTParent.replaceWithVoiceFragment(fragment)
        } else {
            // Use Navigation component fallback if not inside PTTFragment
            when (fragment) {
                is PTTVoiceFragment -> findNavController().navigate(
                    R.id.action_PTTVoiceFragment_to_PTTRecentsFragment
                )
                else -> findNavController().navigate(R.id.action_PTTVoiceFragment_to_PTTFragment)
            }
        }
    }

    fun showBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            back
        )
    }

    private val back = object : OnBackPressedCallback(
        true
    ) {
        override fun handleOnBackPressed() {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
//                requireActivity().onBackPressedDispatcher.onBackPressed()
//                findNavController().navigate(
//                    R.id.action_PTTFragment_to_dialerFragment
//                )
                loadFragment(PTTRecentsFragment())
                // findNavController().popBackStack()
            }
        }

        override fun handleOnBackCancelled() {
            super.handleOnBackCancelled()
        }
    }
}
