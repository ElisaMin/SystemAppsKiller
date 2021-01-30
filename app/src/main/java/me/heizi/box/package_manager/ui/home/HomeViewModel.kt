package me.heizi.box.package_manager.ui.home

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.repositories.AppsPagingSource
import me.heizi.box.package_manager.repositories.PackageRepository

class HomeViewModel : ViewModel() {

    lateinit var repository: PackageRepository
    lateinit var flow:Flow<PagingData<DisplayingData>>
    val adapter by lazy { Adapter() }
    fun start(pm: PackageManager) {
        viewModelScope.launch(Unconfined) {
            launch {
                flow = Pager(config = PagingConfig(20), pagingSourceFactory = {
                    AppsPagingSource(pm)
                }).flow.flowOn(Unconfined)
            }
        }
    }
    fun uninstalling(position:Int) {
        viewModelScope.launch(IO) {

        }
    }


}