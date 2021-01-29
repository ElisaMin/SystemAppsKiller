package me.heizi.box.package_manager.ui.pre_config

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.PreconfigFragmentBinding

class PreconfigFragment : Fragment(R.layout.preconfig_fragment) {
    private val viewModel by viewModels<PreconfigViewModel>()
    private val binding by lazy { PreconfigFragmentBinding.bind(requireView()) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel
    }

    /**
     * First time start
     *
     * 第一次启动时写入mount string和提示免责声明
     */
    private suspend fun firstTimeStart() {
        parent.preferences.mountString = Application.DEFAULT_MOUNT_STRING
        // TODO: 2021/1/28 弹出Dialog提示警告
    }

    /**
     * 通知ViewModel启动
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(IO) {
            val mapper = parent.preferences
            if (mapper.mountString == null) firstTimeStart()
            viewModel.start(mapper.mountString ?: throw NullPointerException("mount string炸裂"))
        }
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(IO) {
            viewModel.flow.collect {
                when(it) {
                    PreconfigViewModel.Status.Done -> {
                        // TODO: 2021/1/28 跳转和保存mount string
                    }
                }
            }
        }
    }


}