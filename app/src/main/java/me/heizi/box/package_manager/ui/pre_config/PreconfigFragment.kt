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
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.utils.DialogBtns
import me.heizi.box.package_manager.utils.dialog
import me.heizi.box.package_manager.utils.set

class PreconfigFragment : Fragment(R.layout.preconfig_fragment) {

    private val viewModel by viewModels<PreconfigViewModel>()
    private val binding by lazy { me.heizi.box.package_manager.databinding.PreconfigFragmentBinding.bind(requireView()) }


    private fun launchOnFirstTime() {
        context?.dialog(
                title = "危险警告",
                cancelable = false,
                message = "本应用可能会造成设备硬件损坏，不对，一定会。想象一下，在你点击root权限给予的时候，突然手机冒烟爆炸，请确认你真的要进入本应用吗？。",
                btns = arrayOf(DialogBtns.Positive("已阅，出错我承担。") { _, _ ->
                    Log.i(TAG, "launchOnFirstTime: starting test clicked")
                    startTesting()
                })

        )
    }

    /**
     * Start testing
     *
     * 开始
     */
    private fun startTesting() = lifecycleScope.launch(Main) {
        launch(IO) {
            viewModel.mountString set DEFAULT_MOUNT_STRING
        }
        launch(Unconfined) {
            viewModel.status.collect {
                Log.i(TAG, "onStart: ${it.javaClass.simpleName}")
                viewModel.deal(it)
                Log.i(TAG, "onStart:${collects++}")
                when(it) {
                    is PreconfigViewModel.Status.Done -> {
                        launch(IO) {
                            parent.preferences.mountString = viewModel.mountString.value
                        }
                        toHome()
                    }
                }
            }
        }
        viewModel.start()
    }

    private fun toHome() {
        lifecycleScope.launch (Dispatchers.Main){
            findNavController().navigate(R.id.action_preconfigFragment_to_homeFragment)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(false) {
            launchOnFirstTime()
        } else {
            toHome()
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


}