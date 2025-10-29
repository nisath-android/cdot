package com.naminfo.cdot_vc.activities.main.about

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.fragments.SecureFragment
import com.naminfo.cdot_vc.activities.navigateToHelp
import com.naminfo.cdot_vc.databinding.FragmentAboutBinding
import org.linphone.core.tools.Log


class AboutFragment : SecureFragment<FragmentAboutBinding>() {
    private lateinit var viewModel: AboutViewModel

    override fun getLayoutId(): Int = R.layout.fragment_about

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[AboutViewModel::class.java]
        binding.viewModel = viewModel

        val backButton = view.findViewById<ImageView>(R.id.back)

        binding.help!!.setOnClickListener {
            navigateToHelp()
        }

        binding.setPrivacyPolicyClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.about_privacy_policy_link))
            )
            try {
                startActivity(browserIntent)
            } catch (se: SecurityException) {
                Log.e("[About] Failed to start browser intent, $se")
            }
        }

        binding.setLicenseClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.about_license_link))
            )
            try {
                startActivity(browserIntent)
            } catch (se: SecurityException) {
                Log.e("[About] Failed to start browser intent, $se")
            }
        }

        binding.setWeblateClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.about_weblate_link))
            )
            try {
                startActivity(browserIntent)
            } catch (se: SecurityException) {
                Log.e("[About] Failed to start browser intent, $se")
            }
        }
    }
}