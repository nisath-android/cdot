/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.naminfo.activities.assistant.fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.GenericFragment
import org.naminfo.activities.assistant.AssistantActivity
import org.naminfo.activities.assistant.viewmodels.GenericLoginViewModel
import org.naminfo.activities.assistant.viewmodels.GenericLoginViewModelFactory
import org.naminfo.activities.assistant.viewmodels.SharedAssistantViewModel
import org.naminfo.activities.main.MainActivity
import org.naminfo.activities.main.viewmodels.DialogViewModel
import org.naminfo.activities.navigateToEchoCancellerCalibration
import org.naminfo.databinding.AssistantGenericAccountLoginFragmentBinding
import org.naminfo.utils.DialogUtils

class GenericAccountLoginFragment : GenericFragment<AssistantGenericAccountLoginFragmentBinding>() {
    private lateinit var sharedAssistantViewModel: SharedAssistantViewModel
    private lateinit var viewModel: GenericLoginViewModel

    override fun getLayoutId(): Int = R.layout.assistant_generic_account_login_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        sharedAssistantViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedAssistantViewModel::class.java]
        }

        binding.loginButton.setOnClickListener {
            val domainValue = viewModel.domain.value.orEmpty()
            Log.i("GenericAccountLoginFragment", "Domain in Login page: $domainValue")

            // Perform your actions with the domainValue here
        }

        binding.cancel.setOnClickListener { v ->
            // val intent = Intent(context, MainActivity::class.java)
            // startActivity(intent)
            goBack()
        }

       /* val check = binding.checkbox1.isChecked
        Log.i("Login", "Check Box checked = $check")
        if (check) {
            binding.domain.visibility = View.VISIBLE
            binding.radio1.visibility = View.VISIBLE
        } else {
            binding.domain.visibility = View.INVISIBLE
            binding.radio1.visibility = View.INVISIBLE
        }*/

        viewModel = ViewModelProvider(
            this,
            GenericLoginViewModelFactory(sharedAssistantViewModel.getAccountCreator(true))
        )[GenericLoginViewModel::class.java]
        binding.viewModel = viewModel

        viewModel.domain.observe(viewLifecycleOwner) {
            Log.i("Login Activity", " Domain : $it ")
        }

        viewModel.leaveAssistantEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                Log.i("Login Activity", "consume block executed")
                corePreferences.defaultDomain = viewModel.domain.value.toString()
                corePreferences.defaultRlsUri = "sip:rls@${viewModel.domain.value}"
                corePreferences.conferenceServerUri = "sip:conference-factory@${corePreferences.defaultDomain }"
                corePreferences.audioVideoConferenceServerUri = "sip:videoconference-factory@${corePreferences.defaultDomain }"
                val isLinphoneAccount = viewModel.domain.value.orEmpty() == corePreferences.defaultDomain
                coreContext.newAccountConfigured(isLinphoneAccount)

                if (coreContext.core.isEchoCancellerCalibrationRequired) {
                    navigateToEchoCancellerCalibration()
                } else {
                    val domainValue = viewModel.domain.value.orEmpty()
                    Log.i("Login Activity", " Domain after login: $domainValue ")
                    if (domainValue.isNotEmpty()) {
                        viewModel.saveSipContacts(requireContext())
                        // coreContext.core.isVideoEnabled = true
                        coreContext.core.isVideoCaptureEnabled = true
                        coreContext.core.isVideoCaptureEnabled = true
                        coreContext.core.currentCall?.currentParams?.isVideoEnabled = true
                        updateVideoActivationPolicy(true)
                    }
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

        viewModel.invalidCredentialsEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val dialogViewModel =
                    DialogViewModel(getString(R.string.assistant_error_invalid_credentials))
                val dialog: Dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

                dialogViewModel.showCancelButton {
                    viewModel.removeInvalidProxyConfig()
                    dialog.dismiss()
                }

                    /*dialogViewModel.showDeleteButton(
                        {
                            viewModel.continueEvenIfInvalidCredentials()
                            dialog.dismiss()
                        },
                        getString(R.string.assistant_continue_even_if_credentials_invalid)
                    )*/

                dialog.show()
            }
        }

        viewModel.onErrorEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { message ->
                (requireActivity() as AssistantActivity).showSnackBar(message)
            }
        }
    }

    private fun updateVideoActivationPolicy(enable: Boolean) {
        val policy = coreContext.core.videoActivationPolicy
        policy.automaticallyInitiate = enable
        policy.automaticallyAccept = enable
        coreContext.core.videoActivationPolicy = policy
    }
}
