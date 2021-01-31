package me.heizi.box.package_manager.ui.pre_config

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.utils.set

class PreconfigFragment : Fragment(R.layout.preconfig_fragment) {
    private val viewModel by viewModels<PreconfigViewModel>()
    private val binding by lazy { me.heizi.box.package_manager.databinding.PreconfigFragmentBinding.bind(requireView()) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(IO) {
            viewModel.mountString.set(parent.preferences.mountString ?: throw NullPointerException("为什么是空的！！！我炸裂"))
        }
        lifecycleScope.launch(Unconfined) {
            viewModel.status.collectLatest {
                Log.i(TAG, "onStart: ${it.javaClass.simpleName}")
            }
        }
        lifecycleScope.launch(Main) {
            viewModel.start()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: called")
    }


}