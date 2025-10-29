package com.naminfo.cdot_vc.activities.main.settings.fragments

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.naminfo.cdot_vc.activities.GenericFragment

abstract class GenericSettingFragment <T : ViewDataBinding> : GenericFragment<T>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        useMaterialSharedAxisXForwardAnimation = sharedViewModel.isSlidingPaneSlideable.value == false

        super.onViewCreated(view, savedInstanceState)
    }
}