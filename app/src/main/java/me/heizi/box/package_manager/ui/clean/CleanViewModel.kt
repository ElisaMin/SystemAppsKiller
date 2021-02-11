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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        fun withBackupTypeAwait(block:(BackupType)->Unit)
        fun longToast(string: String)
        fun getMountString():String
        fun startAndBindService(connection: ServiceConnection)
        fun startedCallback()
    }

    val processing get() = _processing.asStateFlow()
    val adapter by lazy { Adapter() }
    val textInput = msf("")

    val helpText get() = _helpText.asStateFlow()

    val isUninstallable get() =  _isUninstallable.asStateFlow()
    private val _isUninstallable = msf(false)
    private val _processing = msf(true)

    /**
     * Uninstall info
     * 变动时扔到adapter里面展示给用户
     */
    private val _uninstallsInfo:MutableSharedFlow<JsonContent?> = MutableSharedFlow()
    private val _helpText:msf<String?> = msf(null)
    fun startProcessing() {
        _processing set  true
    }
    fun stopProcessing() {
        _processing set false
    }
    init {
        viewModelScope.launch(Default) {
            textInput.collectLatest {
                if (it.length <= 6) {
                    _helpText set null

                    return@collectLatest
                }
                startProcessing()
                try {
                    val jsonContent = Compressor.buildJson(it)
                    launch(Default) {
                        _uninstallsInfo.emit(jsonContent)
                    }
                    Log.i(TAG, "workingwill")
                    _isUninstallable set true
                    _helpText set null
                }catch (e:Exception) {
                    Log.i(TAG, "wrongWithDecoding ",e)
                    _isUninstallable set false
                    _helpText set e.message
                    _uninstallsInfo.emit(null)
                } finally {
                    stopProcessing()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            _uninstallsInfo
                .collectLatest {
                launch(Main) {
                    Log.i(TAG, "list: changed on cleaning")
                    adapter.submitList(it?.apps?.toMutableList())
                }
            }
        }

    }

    /**
     * On done btn clicked
     *
     * 当点击时弹出窗口让用户选择备份模式
     */
    fun onDoneBtnClicked() {
        if (_isUninstallable.value) {
            service.withBackupTypeAwait {
                val list = adapter.currentList
                val mountString = service.getMountString()
                val connect = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        if (binder is CleaningAndroidService.Binder) {
                            viewModelScope.launch {
                                binder.startUninstall(viewModelScope,list,it,mountString)
                                service.startedCallback()
                            }
                        }
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {}
                }
                service.startAndBindService(connect)
            }
        } else {
            service.longToast("似乎列表还没有就绪")
        }
    }
}