package me.heizi.box.package_manager.repositories

import android.content.pm.PackageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import me.heizi.box.package_manager.utils.isUserApp
import me.heizi.kotlinx.shell.OneTimeExecutor

class PackageRepository(
    private val scope: CoroutineScope
) {
    private val uninstallingApps = MutableSharedFlow<PackageInfo>()
    suspend fun PackageInfo.uninstall() = uninstallingApps.emit(this)
    suspend fun uninstalling(packageInfo: PackageInfo) {
        if (packageInfo.applicationInfo.isUserApp) TODO("调用Api弹出卸载窗口")
        else TODO("调用shell 删除或备份")
    }
    suspend fun uninstalling() {
        OneTimeExecutor.getInstance(scope)
    }
}