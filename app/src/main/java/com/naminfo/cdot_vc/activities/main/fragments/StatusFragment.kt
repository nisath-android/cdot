package com.naminfo.cdot_vc.activities.main.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.main.viewmodels.StatusViewModel
import com.naminfo.cdot_vc.databinding.FragmentStatusBinding
import com.naminfo.cdot_vc.utils.Event
import org.linphone.core.tools.Log

class StatusFragment : GenericFragment<FragmentStatusBinding>() {
    private lateinit var viewModel: StatusViewModel

    override fun getLayoutId(): Int = R.layout.fragment_status

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        useMaterialSharedAxisXForwardAnimation = false

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]
        binding.viewModel = viewModel

        sharedViewModel.accountRemoved.observe(
            viewLifecycleOwner
        ) {
            Log.i("[Status Fragment] An account was removed, update default account state")
            val defaultAccount = coreContext.core.defaultAccount
            if (defaultAccount != null) {
                viewModel.updateDefaultAccountRegistrationStatus(defaultAccount.state)
            }
        }

        binding.setMenuClickListener {
            sharedViewModel.toggleDrawerEvent.value = Event(true)
        }

        binding.setRefreshClickListener {
            viewModel.refreshRegister()
        }
    }
}
