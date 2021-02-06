package me.heizi.box.package_manager.ui.clean

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.CleanFragmentBinding

class CleanFragment : Fragment(R.layout.clean_fragment) {
    private val binding by lazy { CleanFragmentBinding.bind(requireView()) }
    private val viewModel by viewModels<CleanViewModel>()




}