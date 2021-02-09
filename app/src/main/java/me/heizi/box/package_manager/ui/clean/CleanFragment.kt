package me.heizi.box.package_manager.ui.clean

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.CleanFragmentBinding
import me.heizi.box.package_manager.utils.dialog

class CleanFragment : Fragment(R.layout.clean_fragment) {
    private val binding by lazy { CleanFragmentBinding.bind(requireView()) }
    private val viewModel:CleanViewModel by viewModels(factoryProducer = { object :ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(CleanViewModel.Service::class.java).newInstance(defaultViewModelService)
        }
    } })
    private val defaultViewModelService = object : CleanViewModel.Service {
        override fun onDoneClicked() {
            requireContext().dialog(
                    
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }




}