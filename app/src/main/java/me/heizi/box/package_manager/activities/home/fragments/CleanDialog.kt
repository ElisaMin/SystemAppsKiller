package me.heizi.box.package_manager.activities.home.fragments

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.home.HomeActivity.Companion.parent
import me.heizi.box.package_manager.activities.home.adapters.EditUninstallListAdapter
import me.heizi.box.package_manager.databinding.DialogCleanBinding
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.CompleteVersion
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.box.package_manager.utils.*
import java.util.*

class CleanDialog : BottomSheetDialogFragment() {
    private val binding by lazy { DialogCleanBinding.bind(requireView()) }
    val viewModel = ViewModel()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return layoutInflater.inflate(R.layout.dialog_clean,container) }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        viewModel.startCollect()
        viewModel.stopProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            viewModel.run {
                try {
                    BitmapFactory.decodeStream(context?.contentResolver!!.openInputStream(it!!)).let { bitmap ->
                        Compressor.read(bitmap)
                    }.let { completeVersion -> onUninstallInfoChanged(completeVersion) }
                    showErrorMessage(null)
                    inuninstallable()
                }catch (e:Exception) {
                    showErrorMessage(e.message)
                    inuninstallable()
                }finally { stopProgress() }
            }
        }.let {
            viewModel.onGetImageClick = {
                viewModel.startProgress()
                it.launch("image/*")
            }
        }
    }


    /**
     * On done btn clicked
     *
     * 当done的按钮点击后判断是否可卸载,并且调用dialog让用户选择type启动bind的程序然后冲
     */
    private fun onDoneBtnClicked() {
        if (viewModel.isUninstallable.value) callDialogGetBackupType {
            val list = viewModel.editUninstallListAdapter.currentList
            val mountString = parent.viewModel.preferences.mountString ?: DEFAULT_MOUNT_STRING
            class C : ServiceConnection {
                val context:Context = parent
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder is CleaningAndroidService.Binder) lifecycleScope.launch(Main) {
                        //调用binder的函数
                        binder.startUninstall(list,it,mountString){
                            context.applicationContext.unbindService(this@C)
                        }
                        context.shortToast("正在开始卸载一共${list.size}个应用")
                        dismiss()
                    } else throw IllegalArgumentException("binder非来自本应用")
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            val connect = C()
            CleaningAndroidService.intent(requireContext().applicationContext).let {i->
                context?.applicationContext?.let {
                    it.startService(i)
                    it.bindService(i,connect, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }

    private fun ViewModel.startCollect() = lifecycleScope.launch(Dispatchers.Default) {
        textInput.collectLatest {
            onInputting(it)
        }
    }

    /**
     * 在文本输入的时候超过六个触发机制检查是否可卸载 否则不可卸载(((
     *
     * @param input
     */
    private fun ViewModel.onInputting(input:String) {
        if (input.length <= 6) { showErrorMessage(null);return } else { startProgress() }
        try {
            val jsonContent = Compressor.read(input)
            onUninstallInfoChanged(jsonContent)
            uninstallable()
            showErrorMessage(null)
        }catch (e:Exception) {
            Log.i(Application.TAG, "wrongWithDecoding ",e)
            inuninstallable()
            showErrorMessage(e.message)
            onUninstallInfoChanged(null)
        } finally {
            stopProgress()
        }
    }

    /**
     * 在uninstall info 改变时往adapter放东西
     *
     * @param version
     */
    private fun ViewModel.onUninstallInfoChanged(version:CompleteVersion?) = lifecycleScope.launch(Main) {
        editUninstallListAdapter.submitList(version?.apps?.toMutableList())
        if (version == null) {
            viewModel.listCount set ""
            viewModel.versionName set ""
        } else {
            viewModel.listCount set "${version.apps.size}个"
            viewModel.versionName set version.name
            uninstallable()
        }
        Log.i(Application.TAG, "list: changed on cleaning")
    }

    /**
     * Call dialog get backup type
     *
     * 弹出窗口让用户选择备份模式后执行[block]回调
     */
    private fun callDialogGetBackupType(block:(BackupType)->Unit) {
        requireContext().dialogBuilder(
                title = "请选择备份模式",
        ).apply {
            setItems(arrayOf("不备份","把apk改名为apk.bak","把APK移动到内部存储空间")) {_,i ->
                when(i) {
                    0 -> BackupType.JustRemove
                    1 -> BackupType.BackupWithOutPath
                    else -> BackupType.BackupWithPath.Default
                }.let(block)
            }
        }.show()
    }

    inner class ViewModel {
        lateinit var onGetImageClick:()->Unit
        val versionName = MutableStateFlow("")
//        val createTime = MutableStateFlow("")
        val listCount = MutableStateFlow("")
        val editUninstallListAdapter: EditUninstallListAdapter by lazy { EditUninstallListAdapter() }
        val textInput: MutableStateFlow<String> = MutableStateFlow("")
        val helpText get() = _helpText.asStateFlow()
        val isUninstallable get() = _isUninstallable.asStateFlow()
        val progressing get() = _progressing.asStateFlow()
        private val _helpText = MutableStateFlow<String?>(null)
        private val _isUninstallable = MutableStateFlow(false)
        private val _progressing = MutableStateFlow(true)
        fun onDoneBtnClicked() { this@CleanDialog.onDoneBtnClicked() }
        fun startProgress() { _progressing set true }
        fun stopProgress() { _progressing set false }
        fun showErrorMessage(error: String?) { _helpText set error }
        fun uninstallable() { _isUninstallable set true }
        fun inuninstallable() { _isUninstallable set false }
    }
}
