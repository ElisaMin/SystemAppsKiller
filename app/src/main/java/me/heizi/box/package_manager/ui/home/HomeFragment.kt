package me.heizi.box.package_manager.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.HomeFragmentBinding

class HomeFragment : Fragment(R.layout.home_fragment) {
    private val viewModel:HomeViewModel by viewModels()
    private val binding by lazy { HomeFragmentBinding.bind(requireView()) }

    override fun onStart() {
        super.onStart()
        viewModel.start(parent.getPackageManager())
        binding.vm = viewModel
    }

}