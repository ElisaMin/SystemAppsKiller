package me.heizi.box.package_manager.ui.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.models.JsonContent
import me.heizi.box.package_manager.utils.Compressor
import me.heizi.box.package_manager.utils.set
import kotlinx.coroutines.flow.MutableStateFlow as msf

class CleanViewModel(
private val service:Service
) : ViewModel() {

    interface Service {
        fun onDoneClicked()
    }

    val processing get() = _processing.asStateFlow()
    val adapter by lazy { Adapter() }
    val textInput = msf("")

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
    init {

        viewModelScope.launch(Dispatchers.Unconfined) {
            textInput.filter {
                it.length>6
            }.collectLatest {
                startProcessing()
                try {
                    launch(Default) {
                        val jsonContent = Compressor.buildJson(it)
                        _uninstallsInfo.emit(jsonContent)
                    }
                }catch (e:Exception) {
                    _helpText set e.message
                } finally {
                    stopProcessing()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            _uninstallsInfo.collectLatest {
                launch(Dispatchers.Main) { adapter.submitList(it.apps.toMutableList()) }
            }
        }
    }


    /**
     * On done btn clicked
     *
     * 当点击时弹出窗口让用户选择备份模式
     * 完成时
     */
    fun onDoneBtnClicked() {
        service.onDoneClicked()
    }
}