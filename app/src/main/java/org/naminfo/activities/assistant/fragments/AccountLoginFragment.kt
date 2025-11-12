package org.naminfo.activities.assistant.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.linphone.core.tools.Log
import org.naminfo.R
import org.naminfo.activities.assistant.viewmodels.AccountLoginViewModel
import org.naminfo.activities.assistant.viewmodels.AccountLoginViewModelFactory
import org.naminfo.activities.assistant.viewmodels.SharedAssistantViewModel
import org.naminfo.activities.main.MainActivity
import org.naminfo.databinding.AssistantAccountLoginFragmentBinding

class AccountLoginFragment : AbstractPhoneFragment<AssistantAccountLoginFragmentBinding>() {
    override lateinit var viewModel: AccountLoginViewModel
    private lateinit var sharedAssistantViewModel: SharedAssistantViewModel

    override fun getLayoutId(): Int = R.layout.assistant_account_login_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        sharedAssistantViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedAssistantViewModel::class.java]
        }

        viewModel = ViewModelProvider(
            this,
            AccountLoginViewModelFactory(sharedAssistantViewModel.getAccountCreator())
        )[AccountLoginViewModel::class.java]
        binding.viewModel = viewModel

        binding.setForgotPasswordClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse(getString(R.string.assistant_forgotten_password_link))
            startActivity(intent)
        }

        viewModel.loginEvent.observe(
            viewLifecycleOwner,
            Observer { event ->
                event?.consume { isSuccess ->
                    if (isSuccess) {
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Show login failure message if needed
                        Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        /*binding.setInfoClickListener {
            showPhoneNumberInfoDialog()
        }

        binding.setSelectCountryClickListener {
            val countryPickerFragment = CountryPickerFragment()
            countryPickerFragment.listener = viewModel
            countryPickerFragment.show(childFragmentManager, "CountryPicker")
        }


        viewModel.prefix.observe(viewLifecycleOwner) { internationalPrefix ->
            viewModel.getCountryNameFromPrefix(internationalPrefix)
        }

        viewModel.goToSmsValidationEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                val args = Bundle()
                args.putBoolean("IsLogin", true)
                args.putString("PhoneNumber", viewModel.accountCreator.phoneNumber)
                navigateToPhoneAccountValidation(args)
            }
        }

        viewModel.leaveAssistantEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                coreContext.newAccountConfigured(true)

                if (coreContext.core.isEchoCancellerCalibrationRequired) {
                    navigateToEchoCancellerCalibration()
                } else {
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

                dialogViewModel.showDeleteButton(
                    {
                        viewModel.continueEvenIfInvalidCredentials()
                        dialog.dismiss()
                    },
                    getString(R.string.assistant_continue_even_if_credentials_invalid)
                )

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

        checkPermissions()
    }*/
    }

    fun getLocalIP(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.dhcpInfo
            val ipAddress = info.ipAddress

            // Convert the integer IP address to string format
            val ip = Formatter.formatIpAddress(ipAddress)
            Log.e("AccountLoginViewModel", "Wifi IP address is: $ip")
            ip
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
