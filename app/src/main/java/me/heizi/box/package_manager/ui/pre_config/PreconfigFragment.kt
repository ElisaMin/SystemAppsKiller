package me.heizi.box.package_manager.ui.pre_config

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.utils.dialog

class PreconfigFragment : Fragment(R.layout.preconfig_fragment) {
    private val viewModel by viewModels<PreconfigViewModel>()
    private val binding by lazy { me.heizi.box.package_manager.databinding.PreconfigFragmentBinding.bind(requireView()) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vm = viewModel
    }

    /**
     * First time start
     *
     * 第一次启动时写入mount string和提示免责声明
     */
    private suspend fun firstTimeStart(onDone:()->Unit) {
        parent.preferences.mountString = Application.DEFAULT_MOUNT_STRING
        lifecycleScope.launch(Main) {
            context?.dialog(
                    title = "危险警告",
                    message = "当你给予Root授权后会立即造成硬件损坏，请酌情使用。"
            )
            onDone()
        }

    }

    /**
     * 通知ViewModel启动
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(IO) {
            val mapper = parent.preferences
            if (mapper.mountString == null) firstTimeStart() {
                viewModel.start(mapper.mountString ?: throw NullPointerException("mount string炸裂"))
            }else viewModel.start(mapper.mountString ?: throw NullPointerException("mount string炸裂"))

        }
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(IO) {
            viewModel.flow.collectLatest {
                when(it) {
                    PreconfigViewModel.Status.Done -> {
                        // TODO: 2021/1/28 跳转和保存mount string
                    }
                }
            }
        }
    }


}