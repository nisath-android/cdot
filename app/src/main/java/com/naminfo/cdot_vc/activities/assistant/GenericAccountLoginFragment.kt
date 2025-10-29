package com.naminfo.cdot_vc.activities.assistant

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.assistant.viewmodels.GenericLoginViewModel
import com.naminfo.cdot_vc.activities.assistant.viewmodels.GenericLoginViewModelFactory
import com.naminfo.cdot_vc.activities.main.MainActivity
import com.naminfo.cdot_vc.activities.main.viewmodels.DialogViewModel
import com.naminfo.cdot_vc.databinding.FragmentGenericAccountLoginBinding
import com.naminfo.cdot_vc.utils.DialogUtils
import com.naminfo.cdot_vcactivities.assistant.viewmodels.SharedAssistantViewModel

class GenericAccountLoginFragment : GenericFragment<FragmentGenericAccountLoginBinding>() {
    private lateinit var sharedAssistantViewModel: SharedAssistantViewModel
    private lateinit var viewModel: GenericLoginViewModel

    override fun getLayoutId(): Int = R.layout.fragment_generic_account_login

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

        binding.cancel.setOnClickListener {
            // val intent = Intent(context, MainActivity::class.java)
            // startActivity(intent)
            goBack()
        }

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
                val isLinphoneAccount = viewModel.domain.value.orEmpty() == corePreferences.defaultDomain
                Log.i("Login Activity", "isLinphoneAccount: $isLinphoneAccount ")
                coreContext.newAccountConfigured(isLinphoneAccount)

                    val domainValue = viewModel.domain.value.orEmpty()
                    Log.i("Login Activity", " Domain after login: $domainValue ")

                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()

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
