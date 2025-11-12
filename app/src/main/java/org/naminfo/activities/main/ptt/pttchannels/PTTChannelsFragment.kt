package org.naminfo.activities.main.ptt.pttchannels

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.naminfo.R
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.databinding.FragmentPttChannelsBinding

class PTTChannelsFragment : SecureFragment<FragmentPttChannelsBinding>() {

    companion object {
        fun newInstance() = PTTChannelsFragment()
    }

    private val viewModel: PTTChannelsViewModel by viewModels()
    override fun getLayoutId(): Int {
        return R.layout.fragment_ptt_channels
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
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
                // requireActivity().onBackPressedDispatcher.onBackPressed()
                /* findNavController().navigate(
                     R.id.action_takeConfrenceFragment_to_dialerFragment
                 )*/
                findNavController().popBackStack()
            }
        }

        override fun handleOnBackCancelled() {
            super.handleOnBackCancelled()
        }
    }
}
