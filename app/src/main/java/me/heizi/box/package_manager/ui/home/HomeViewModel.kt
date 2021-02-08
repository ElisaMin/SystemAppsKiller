package me.heizi.box.package_manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.set


class HomeViewModel(
    private val repository: PackageRepository
) : ViewModel() {


    val adapter by lazy {
        Adapter(
            viewModelScope,
            repository.defaultAdapterService,
            stopProcessing = {_processing set  false},
            processing = { _processing set true }
        )
    }


    val processing get() = _processing.asStateFlow()
    private val _processing = MutableStateFlow(true)


    fun stopProcess() {
        _processing set false
    }


}