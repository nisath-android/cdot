package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.settings.viewmodels.NetworkSettingsViewModel
import com.naminfo.cdot_vc.databinding.FragmentNetworkSettingsBinding


class NetworkSettingsFragment : GenericSettingFragment<FragmentNetworkSettingsBinding>() {
    private lateinit var viewModel: NetworkSettingsViewModel

    override fun getLayoutId(): Int = R.layout.fragment_network_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel

        viewModel = ViewModelProvider(this)[NetworkSettingsViewModel::class.java]
        binding.viewModel = viewModel

        // android.util.Log.i ("[Network Setting]")
    }
}