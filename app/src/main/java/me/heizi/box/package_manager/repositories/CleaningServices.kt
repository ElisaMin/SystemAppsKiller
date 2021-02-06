package me.heizi.box.package_manager.repositories

import kotlinx.coroutines.flow.MutableSharedFlow
import me.heizi.box.package_manager.models.UninstallInfo


interface CleaningServices {
    sealed class Result {

    }
    val resultFlow:MutableSharedFlow<Result>
    suspend fun uninstalling(uninstallInfo: UninstallInfo)

}