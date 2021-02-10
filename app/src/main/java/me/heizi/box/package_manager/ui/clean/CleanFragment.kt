package me.heizi.box.package_manager.ui.clean

import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.SingletonActivity.Companion.parent
import me.heizi.box.package_manager.databinding.CleanFragmentBinding
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.box.package_manager.utils.dialogBuilder
import me.heizi.box.package_manager.utils.longToast

class CleanFragment : Fragment(R.layout.clean_fragment) {
    private val binding by lazy { CleanFragmentBinding.bind(requireView()) }
    private val viewModel:CleanViewModel by viewModels(factoryProducer = { object :ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(CleanViewModel.Service::class.java).newInstance(defaultViewModelService)
        }
    } })

    private val defaultViewModelService = object : CleanViewModel.Service {
        override fun getBackupType() = callDialogGetBackupType()
        override fun longToast(string: String) { context?.longToast(string) }
        override fun getMountString(): String = parent.viewModel.preferences.mountString ?: DEFAULT_MOUNT_STRING
        override fun startAndBindService(connection: ServiceConnection) {
            CleaningAndroidService.intent(requireContext()).let {i->
                context?.let {
                    it.startService(i)
                    it.bindService(i,connection, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        viewModel.stopProcessing()
    }

    private fun callDialogGetBackupType():BackupType {
        var type:BackupType? = null
        requireContext().dialogBuilder(
                title = "请选择备份模式",
        ).apply {
            setItems(arrayOf("不备份","把apk改名为apk.bak","把APK移动到内部存储空间")) {_,i ->
                type = when(i) {
                    0 -> BackupType.JustRemove
                    1 -> BackupType.BackupWithOutPath
                    else -> BackupType.BackupWithPath.Default
                }
            }
        }.show()
        return  type!!
    }
}
