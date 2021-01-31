package me.heizi.box.package_manager

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.utils.dialog

class SingletonActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SingletonActivity"
        val Fragment.parent get() = this.requireActivity() as SingletonActivity
        val Fragment.app get() = this.parent.application as Application
    }

    val viewModel:SingletonViewModel by viewModels()
    val preferences by lazy { PreferencesMapper(this) }

    private fun firstTimeLaunch() = runBlocking(lifecycleScope.coroutineContext){
        dialog(
            title = "请想好再做行动",
            message = "你正在打开一个潘多拉魔盒，在root授权后可能会造成硬件损坏，如果您接受不了这个结果，请拒绝本应用的Root申请，然后退出应用。"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.containner)
        Log.i(TAG, "onCreate: called")
        if (preferences.mountString == null) {
            lifecycleScope.launch(IO) {
                preferences.mountString = Application.DEFAULT_MOUNT_STRING
            }
            firstTimeLaunch()
        }
    }
}