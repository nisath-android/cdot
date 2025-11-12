package org.naminfo.activities.main.ptt

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.naminfo.R
import org.naminfo.activities.main.fragments.SecureFragment
import org.naminfo.activities.main.ptt.pttchannels.PTTChannelsFragment
import org.naminfo.activities.main.ptt.pttcontacts.PTTContactsFragment
import org.naminfo.activities.main.ptt.pttrecents.PTTRecentsFragment
import org.naminfo.databinding.PttFragmentBinding

class PTTFragment : SecureFragment<PttFragmentBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.ptt_fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        if (savedInstanceState == null) {
            loadFragment(PTTContactsFragment())
        }
        setupBottomNav()
        showBackPress()
        setBackpress()
    }

    private fun setBackpress() {
        binding.btnBack.setOnClickListener {
            findNavController().navigate(
                R.id.action_PTTFragment_to_dialerFragment
            )
        }
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ptt_recents -> loadFragment(PTTRecentsFragment())
                R.id.nav_ptt_contacts -> loadFragment(PTTContactsFragment())
                R.id.nav_ptt_channels -> loadFragment(PTTChannelsFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    fun replaceWithVoiceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
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
                //  requireActivity().onBackPressedDispatcher.onBackPressed()
                findNavController().navigate(
                    R.id.action_PTTFragment_to_dialerFragment
                )
                // findNavController().popBackStack()
            }
        }

        override fun handleOnBackCancelled() {
            super.handleOnBackCancelled()
        }
    }
}
