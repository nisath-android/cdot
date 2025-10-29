package com.naminfo.cdot_vc.activities.main.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.main.viewmodels.TabsViewModel
import com.naminfo.cdot_vc.activities.navigateToCallHistory
import com.naminfo.cdot_vc.activities.navigateToContacts
import com.naminfo.cdot_vc.activities.navigateToDialer
import com.naminfo.cdot_vc.databinding.FragmentTabsBinding
import com.naminfo.cdot_vc.utils.Event


class TabsFragment: GenericFragment<FragmentTabsBinding>(), NavController.OnDestinationChangedListener {
    private lateinit var viewModel: TabsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_tabs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        useMaterialSharedAxisXForwardAnimation = false

        viewModel = requireActivity().run {
            ViewModelProvider(this)[TabsViewModel::class.java]
        }
        binding.viewModel = viewModel

        binding.setHistoryClickListener {
            when (findNavController().currentDestination?.id) {
                R.id.masterContactsFragment -> sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                    R.id.masterCallLogsFragment
                )
                R.id.dialerFragment -> sharedViewModel.updateDialerAnimationsBasedOnDestination.value = Event(
                    R.id.masterCallLogsFragment
                )
            }
            navigateToCallHistory()
        }

       binding.setContactsClickListener {
            when (findNavController().currentDestination?.id) {
                R.id.dialerFragment -> sharedViewModel.updateDialerAnimationsBasedOnDestination.value = Event(
                    R.id.masterContactsFragment
                )
            }
            sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                findNavController().currentDestination?.id ?: -1
            )
            navigateToContacts()
        }

        binding.setDialerClickListener {
            when (findNavController().currentDestination?.id) {
                R.id.masterContactsFragment -> sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                    R.id.dialerFragment
                )
            }
            sharedViewModel.updateDialerAnimationsBasedOnDestination.value = Event(
                findNavController().currentDestination?.id ?: -1
            )
            navigateToDialer()
        }

       /* binding.setChatClickListener {
            when (findNavController().currentDestination?.id) {
                R.id.masterContactsFragment -> sharedViewModel.updateContactsAnimationsBasedOnDestination.value = Event(
                    R.id.masterChatRoomsFragment
                )
                R.id.dialerFragment -> sharedViewModel.updateDialerAnimationsBasedOnDestination.value = Event(
                    R.id.masterChatRoomsFragment
                )
            }
            navigateToChatRooms()
        }*/
    }

    override fun onStart() {
        super.onStart()
        findNavController().addOnDestinationChangedListener(this)
    }

    override fun onStop() {
        findNavController().removeOnDestinationChangedListener(this)
        super.onStop()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        if (corePreferences.enableAnimations) {
            when (destination.id) {
                R.id.masterContactsFragment ->
                    binding.motionLayout.transitionToState(R.id.contacts_set)

                R.id.dialerFragment ->
                    binding.motionLayout.transitionToState(R.id.dialer_set)
            }
        } else {
            when (destination.id) {
                R.id.masterContactsFragment ->
                    binding.motionLayout.setTransition(R.id.contacts_set, R.id.contacts_set)

                R.id.dialerFragment ->
                    binding.motionLayout.setTransition(R.id.dialer_set, R.id.dialer_set)
            }
        }
    }
}
