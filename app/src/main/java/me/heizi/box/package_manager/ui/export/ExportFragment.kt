package me.heizi.box.package_manager.ui.export

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ExportFragmentBinding

class ExportFragment : Fragment(R.layout.export_fragment) {
    private val binding by lazy { ExportFragmentBinding.bind(requireView()) }
    private val viewModel:ExportViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

}