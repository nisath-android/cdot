package com.naminfo.cdot_vc.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.main.viewmodels.SharedMainViewModel
import org.linphone.core.tools.Log

abstract class GenericFragment<T : ViewDataBinding> : Fragment() {
    companion object {
        val emptyFragmentsIds = arrayListOf(
            R.id.emptyChatFragment,
            R.id.emptyContactFragment,
            R.id.emptySettingsFragment,
            R.id.emptyCallHistoryFragment
        )
    }

    private var _binding: T? = null
    protected val binding get() = _binding!!

    protected var useMaterialSharedAxisXForwardAnimation = true

    protected lateinit var sharedViewModel: SharedMainViewModel

    protected fun isSharedViewModelInitialized(): Boolean {
        return ::sharedViewModel.isInitialized
    }

    protected fun isBindingAvailable(): Boolean {
        return _binding != null
    }

    private fun getFragmentRealClassName(): String {
        // return this.javaClass.simpleName // Returns only class name instead of full package
        return this::class.java.simpleName // Returns only class name instead of full package
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            try {
                val navController = findNavController()
                Log.i("[Generic Fragment] ${getFragmentRealClassName()} handleOnBackPressed")
                isEnabled = false
                // Check if the current fragment is WelcomeFragment
                when (getFragmentRealClassName()) {
                    "WelcomeFragment" -> {
                        isEnabled = true
                        Log.i("[Generic Fragment] WelcomeFragment detected, closing app.")
                        requireActivity().finishAffinity() // Close the app
                    }
                    "GenericAccountLoginFragment" -> {
                        isEnabled = true
                        Log.i("[Generic Fragment] WelcomeFragment detected, closing app.")
                        requireActivity().finishAffinity() // Close the app
                    }
                    else -> {
                        // Navigate back normally
                        if (!navController.popBackStack()) {
                            Log.i("[Generic Fragment] ${getFragmentRealClassName()} couldn't pop")
                            if (!navController.navigateUp()) {
                                Log.i(
                                    "[Generic Fragment] ${getFragmentRealClassName()} couldn't navigate up"
                                )
                                goBack()
                            }
                        }
                    }
                }
            } catch (ise: IllegalStateException) {
                Log.e(
                    "[Generic Fragment] ${getFragmentRealClassName()} handleOnBackPressed() Error: $ise"
                )
            }
        }
    }

    abstract fun getLayoutId(): Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedMainViewModel::class.java]
        }

        sharedViewModel.isSlidingPaneSlideable.observe(viewLifecycleOwner) {
            Log.d(
                "[Generic Fragment] ${getFragmentRealClassName()} shared main VM sliding pane has changed"
            )
            //onBackPressedCallback.isEnabled = backPressedCallBackEnabled()
        }

        _binding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)

        return _binding!!.root
    }

    override fun onStart() {
        super.onStart()

        if (useMaterialSharedAxisXForwardAnimation && corePreferences.enableAnimations) {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

            postponeEnterTransition()
            binding.root.doOnPreDraw { startPostponedEnterTransition() }
        }

        setupBackPressCallback()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        _binding = null
    }

    protected fun goBack() {
        try {
            requireActivity().onBackPressedDispatcher.onBackPressed()
  /*           val action = when (findNavController().currentDestination?.id) {
          R.id.settingsFragment -> R.id.action_global_settingsFragment
          R.id.accountSettingsFragment -> R.id.action_settingsFragment_to_dialerFragment
          else -> R.id.action_global_dialerFragment
      }
      findNavController().navigate(
          action,
          null,
          popupTo(R.id.dialerFragment, true)
      )*/

        } catch (ise: IllegalStateException) {
            Log.w("[Generic Fragment] ${getFragmentRealClassName()}.goBack() can't go back: $ise")
            onBackPressedCallback.handleOnBackPressed()
        }
    }



    private fun setupBackPressCallback() {
        Log.d("[Generic Fragment] ${getFragmentRealClassName()} setupBackPressCallback")

        val backButton = binding.root.findViewById<ImageView>(R.id.back)

        if (backButton != null) {
            // backButton.visibility = View.GONE
            if (getFragmentRealClassName() == "GenericAccountLoginFragment" || getFragmentRealClassName() == "TopBarFragment") {
                backButton.visibility = View.GONE
            } else {
                backButton.visibility = View.VISIBLE
            }
            Log.i("[Generic Fragment] ${getFragmentRealClassName()} found back button")
            onBackPressedCallback.isEnabled = backPressedCallBackEnabled()
            backButton.setOnClickListener {
                goBack()
            }
        } else {
            onBackPressedCallback.isEnabled = false
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    private fun backPressedCallBackEnabled(): Boolean {
        if (findNavController().graph.id == R.id.main_nav_graph) return false

        val isSlidingPaneFlat = sharedViewModel.isSlidingPaneSlideable.value == false
        Log.d(
            "[Generic Fragment] ${getFragmentRealClassName()} isSlidingPaneFlat ? $isSlidingPaneFlat"
        )

        val isPreviousFragmentEmpty =
            findNavController().previousBackStackEntry?.destination?.id in emptyFragmentsIds
        Log.d(
            "[Generic Fragment] ${getFragmentRealClassName()} isPreviousFragmentEmpty ? $isPreviousFragmentEmpty"
        )

        val popBackStack = isSlidingPaneFlat || !isPreviousFragmentEmpty
        Log.d("[Generic Fragment] ${getFragmentRealClassName()} popBackStack ? $popBackStack")
        return popBackStack
    }
}
