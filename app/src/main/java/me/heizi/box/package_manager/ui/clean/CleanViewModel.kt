package me.heizi.box.package_manager.ui.clean

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.JsonContent
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.box.package_manager.utils.Compressor
import me.heizi.box.package_manager.utils.set
import kotlinx.coroutines.flow.MutableStateFlow as msf

class CleanViewModel(
private val service:Service
) : ViewModel() {

    interface Service {
        fun getBackupType():BackupType
        fun longToast(string: String)
        fun getMountString():String
        fun startAndBindService(connection: ServiceConnection)
    }

    val processing get() = _processing.asStateFlow()
    val adapter by lazy { Adapter() }
    val textInput = msf("")

    val helpText get() = _helpText.asStateFlow()

    private val _processing = msf(true)

    /**
     * Uninstall info
     * 变动时扔到adapter里面展示给用户
     */
    private val _uninstallsInfo:MutableSharedFlow<JsonContent> = MutableSharedFlow()
    private val _helpText:msf<String?> = msf(null)
    fun startProcessing() {
        _processing set  true
    }
    fun stopProcessing() {
        _processing set false
    }
    var isUsing = false
    init {
        viewModelScope.launch(Dispatchers.Default) {
            textInput.filter {
                it.length>6
            }.collectLatest {
                startProcessing()
                try {
                    val jsonContent = Compressor.buildJson(it)
                    launch(Default) {
                        _uninstallsInfo.emit(jsonContent)
                    }
                    Log.i(TAG, "workingwill")
                    isUsing = true
                }catch (e:Exception) {
                    Log.i(TAG, "wrongWithDecoding ",e)
                    isUsing = false
                    _helpText set e.message
                } finally {
                    stopProcessing()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            _uninstallsInfo.collectLatest {
                launch(Main) { adapter.submitList(it.apps.toMutableList()) }
            }
        }

    }

    /**
     * On done btn clicked
     *
     * 当点击时弹出窗口让用户选择备份模式
     */
    fun onDoneBtnClicked() {
        Log.i(TAG, "onDoneBtnClicked: $isUsing")
        if (isUsing) {
            val task = viewModelScope.async(Main){ service.getBackupType() }
            val list = adapter.currentList
            val mountString = service.getMountString()
            val connect = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (service is CleaningAndroidService.Binder) {
                        viewModelScope.launch {
                            task.await().let {
                                service.startUninstall(viewModelScope,list,it,mountString)
                            }
                        }
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            service.startAndBindService(connect)
        } else {
            service.longToast("似乎列表还没有就绪")
        }
    }
}