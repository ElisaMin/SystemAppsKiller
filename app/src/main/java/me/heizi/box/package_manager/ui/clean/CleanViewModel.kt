package me.heizi.box.package_manager.ui.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.Compressor
import me.heizi.box.package_manager.utils.set
import kotlinx.coroutines.flow.MutableStateFlow as msf

class CleanViewModel(
//    private val repository: CleanRepository
) : ViewModel() {


    val processing get() = _processing.asStateFlow()
    val adapter by lazy { Adapter() }
    val textInput = msf("")

    private val _processing = msf(true)
    private val _list:msf<MutableList<UninstallInfo>> = msf(arrayListOf())
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
                        _list set (Compressor.buildJson(it).apps.toMutableList())
                    }
                }catch (e:Exception) {
                    _helpText set e.message
                } finally {
                    stopProcessing()
                }
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            _list.collectLatest {
                launch(Dispatchers.Main) { adapter.submitList(it) }
            }
        }
    }


    fun onDoneBtnClicked() {
        adapter.currentList.takeUnless { it.isNullOrEmpty() }?.let(::onStartingUninstall)
    }
    private fun onStartingUninstall(list: MutableList<UninstallInfo>){


    }

}