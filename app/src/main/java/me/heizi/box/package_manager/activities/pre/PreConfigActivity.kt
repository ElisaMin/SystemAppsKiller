package me.heizi.box.package_manager.activities.pre

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.activities.home.HomeActivity
import me.heizi.box.package_manager.databinding.ActivityPreConfigBinding
import me.heizi.box.package_manager.utils.DialogBtns
import me.heizi.box.package_manager.utils.dialog
import me.heizi.box.package_manager.utils.set

class PreConfigActivity :AppCompatActivity() {
    private val binding by lazy { ActivityPreConfigBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<PreConfigViewModel>()

    /**
     * Launch on first time
     *
     * 第一次启动
     */
    private fun launchOnFirstTime() {
        dialog(
            DialogBtns.Positive("已阅，出错我承担。") { _, _ ->
                Log.i(TAG, "launchOnFirstTime: starting test clicked")
                startTesting()
            }, title = "危险警告", cancelable = false,
            message = "本应用可能会造成设备硬件损坏，不对，一定会。想象一下，在你点击root权限给予的时候，突然手机冒烟爆炸，请确认你真的要进入本应用吗？。",
        )
    }

    /**
     * Start testing
     *
     * 开始
     */
    private fun startTesting() = lifecycleScope.launch(Main) {
        binding.textInputLayout.setEndIconOnClickListener {
            viewModel.onInputSubmit()
        }
        launch(IO) {
            viewModel.mountString set DEFAULT_MOUNT_STRING
        }
        launch(Unconfined) {
            viewModel.status.collect {
                Log.i(TAG, "onStart: ${it.javaClass.simpleName}")
                viewModel.deal(it)
                Log.i(TAG, "onStart:${collects++}")
                when(it) {
                    is PreConfigViewModel.Status.Done -> {
                        launch(IO) {
                            app.preferenceMapper.mountString =  viewModel.mountString.value
                        }
                        toHome()
                    }
                }
            }
        }
        viewModel.start()
    }
    private fun toHome() = lifecycleScope.launch (Main){
        Intent(this@PreConfigActivity,HomeActivity::class.java).let {
//            it.putExtra()
            startActivity(it)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(app.preferenceMapper.mountString == null) {
//        if(false) {
            launchOnFirstTime()
        } else {
            toHome()
        }
        setContentView(binding.root)
        binding.vm = viewModel
        binding.lifecycleOwner = this
    }

    var collects = 0
}