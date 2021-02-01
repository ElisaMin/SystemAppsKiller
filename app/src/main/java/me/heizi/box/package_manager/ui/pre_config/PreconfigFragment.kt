package me.heizi.box.package_manager.ui.pre_config

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.utils.set

class PreconfigFragment : Fragment(R.layout.preconfig_fragment) {
    private val viewModel by viewModels<PreconfigViewModel>()
    private val binding by lazy { me.heizi.box.package_manager.databinding.PreconfigFragmentBinding.bind(requireView()) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Default) {
            Log.i(TAG, "onStart: collect before")
            viewModel.status.collect {
                Log.i(TAG, "onStart: ${it.javaClass.simpleName}")
                viewModel.deal(it)
                Log.i(TAG, "onStart:${collects++}")
                when(it) {
                    is PreconfigViewModel.Status.Done -> {
                        launch(IO) {
                            parent.preferences.mountString = viewModel.mountString.value
                        }
                        launch (Main){
                            findNavController().navigateUp()
                            findNavController().navigate(R.id.action_preconfigFragment_to_homeFragment)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        binding.textInputLayout.setEndIconOnClickListener {
            viewModel.onInputSubmit()
        }
    }

    var collects = 0
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(IO) {
            viewModel.mountString.set(parent.preferences.mountString ?: throw NullPointerException("为什么是空的！！！我炸裂"))
            viewModel.start()
        }
    }


}