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

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.R
import org.naminfo.activities.*
import org.naminfo.activities.assistant.viewmodels.WelcomeViewModel
import org.naminfo.activities.navigateToAccountLogin
import org.naminfo.activities.navigateToEmailAccountCreation
import org.naminfo.activities.navigateToRemoteProvisioning
import org.naminfo.databinding.AssistantWelcomeFragmentBinding
import org.naminfo.utils.LinphoneUtils

class WelcomeFragment : GenericFragment<AssistantWelcomeFragmentBinding>() {
    private lateinit var viewModel: WelcomeViewModel

    override fun getLayoutId(): Int = R.layout.assistant_welcome_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("[Assistant] onViewCreated")
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[WelcomeViewModel::class.java]
        binding.viewModel = viewModel
        binding.topBarFragment.visibility = View.GONE
        binding.setCreateAccountClickListener {
            if (LinphoneUtils.isPushNotificationAvailable()) {
                Log.i("[Assistant] Core says push notifications are available")
                val deviceHasTelephonyFeature = coreContext.context.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_TELEPHONY
                )
                if (!deviceHasTelephonyFeature) {
                    Log.i(
                        "[Assistant] Device doesn't have TELEPHONY feature, showing email based account creation"
                    )
                    navigateToEmailAccountCreation()
                } else {
                    Log.i(
                        "[Assistant] Device has TELEPHONY feature, showing phone based account creation"
                    )
                    navigateToPhoneAccountCreation()
                }
            } else {
                Log.w(
                    "[Assistant] Failed to get push notification info, showing warning instead of phone based account creation"
                )
                navigateToNoPushWarning()
            }
        }

        binding.setAccountLoginClickListener {
            Log.i("[Assistant] setAccountLoginClickListener")
            navigateToAccountLogin()
        }

        binding.setGenericAccountLoginClickListener {
            Log.i("[Assistant] setGenericAccountLoginClickListener")
            navigateToGenericLoginWarning()
        }

        binding.setRemoteProvisioningClickListener {
            Log.i("[Assistant] setRemoteProvisioningClickListener")
            navigateToRemoteProvisioning()
        }

        /*viewModel.termsAndPrivacyAccepted.observe(
            viewLifecycleOwner
        ) {
            if (it) corePreferences.readAndAgreeTermsAndPrivacy = true
        }

        setUpTermsAndPrivacyLinks()*/
    }

    /*@SuppressLint("StringFormatInvalid")
    private fun setUpTermsAndPrivacyLinks() {
        val terms = getString(R.string.assistant_general_terms)
        val privacy = getString(R.string.assistant_privacy_policy)

        val label = try {
            getString(
                R.string.assistant_read_and_agree_terms,
                terms,
                privacy
            )
        } catch (e: UnknownFormatConversionException) {
            Log.e("[Welcome] Wrong R.string.assistant_read_and_agree_terms format!")
            "I accept Belledonne Communications' terms of use and privacy policy"
        }
        val spannable = SpannableString(label)

        val termsMatcher = Pattern.compile(terms).matcher(label)
        if (termsMatcher.find()) {
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.assistant_general_terms_link))
                    )
                    try {
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Log.e("[Welcome] Can't start activity: $e")
                    }
                }
            }
            spannable.setSpan(
                clickableSpan,
                termsMatcher.start(0),
                termsMatcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val policyMatcher = Pattern.compile(privacy).matcher(label)
        if (policyMatcher.find()) {
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.assistant_privacy_policy_link))
                    )
                    try {
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        Log.e("[Welcome] Can't start activity: $e")
                    }
                }
            }
            spannable.setSpan(
                clickableSpan,
                policyMatcher.start(0),
                policyMatcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // binding.termsAndPrivacy.text = spannable
        // binding.termsAndPrivacy.movementMethod = LinkMovementMethod.getInstance()
    }*/
}
