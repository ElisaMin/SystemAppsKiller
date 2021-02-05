package me.heizi.box.package_manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.set


class HomeViewModel(
    private val repository: PackageRepository
) : ViewModel() {


    val adapter by lazy {
        Adapter(
            viewModelScope,
            repository.defaultAdapterService
        ) {
            _processing set true
        }
    }

    val processing get() = _processing.asStateFlow()
    private val _processing = MutableStateFlow(true)

    init {
        viewModelScope.launch(Dispatchers.Unconfined) {
            _processing.emit(false)
            repository.systemAppsFlow.collectLatest {
                _processing.emit(true)
                launch(Main) {
                    adapter.notifyDataSetChanged()
                    _processing.emit(false)
                }
            }
        }
    }



    fun stopProcess() {
        _processing set false
    }


}