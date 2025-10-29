package com.naminfo.cdot_vc.activities.assistant

import android.os.Bundle
import android.view.View
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.databinding.AssistantTopBarFragmentBinding

class TopBarFragment : GenericFragment<AssistantTopBarFragmentBinding>() {
    override fun getLayoutId(): Int = R.layout.assistant_top_bar_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        useMaterialSharedAxisXForwardAnimation = false
    }
}