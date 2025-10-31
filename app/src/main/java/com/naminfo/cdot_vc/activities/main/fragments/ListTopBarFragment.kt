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
package com.naminfo.cdot_vc.activities.main.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.activities.GenericFragment
import com.naminfo.cdot_vc.activities.main.viewmodels.ListTopBarViewModel
import com.naminfo.cdot_vc.databinding.FragmentListEditTopBarBinding
import com.naminfo.cdot_vc.utils.Event


class ListTopBarFragment : GenericFragment<FragmentListEditTopBarBinding>() {
    private lateinit var viewModel: ListTopBarViewModel

    override fun getLayoutId(): Int = R.layout.fragment_list_edit_top_bar


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        useMaterialSharedAxisXForwardAnimation = false

        viewModel = ViewModelProvider(parentFragment ?: this)[ListTopBarViewModel::class.java]
        binding.viewModel = viewModel

        binding.setCancelClickListener {
            viewModel.isEditionEnabled.value = false
        }

        binding.setSelectAllClickListener {
            viewModel.selectAllEvent.value = Event(true)
        }

        binding.setUnSelectAllClickListener {
            viewModel.unSelectAllEvent.value = Event(true)
        }

        binding.setDeleteClickListener {
            viewModel.deleteSelectionEvent.value = Event(true)
        }
    }
}
