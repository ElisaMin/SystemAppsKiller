package me.heizi.box.package_manager.repositories

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PackageRepository(
    private val scope: CoroutineScope,
    private val pm:PackageManager
) {
//    private val systemPackages get() =  pm.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY)
    private val systemApps get()  = pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
    private val sortedSystemApps get() = systemApps.apply { sortBy { it.sourceDir } }

    private val _systemAppsFlow by lazy { MutableStateFlow(sortedSystemApps) }
    val systemAppsFlow get() = _systemAppsFlow.asStateFlow()

    companion object {
        private val withApk by lazy { """[/\w+]+/.+\.apk""".toRegex() }
        private val hasNoApk by lazy { """[/\w+]+""".toRegex() }

        fun getPreviousPath(path:String):String {
            fun notNormalPath(): Nothing = throw IllegalArgumentException("$path 非正常path")
            //如果是空或者没有/就直接爆炸
            val list = if (path.isEmpty() || !path.contains("/")) {
                notNormalPath()
            } else path.split("/",ignoreCase = true).toMutableList()
            //带apk的目录删掉.(/./.\.apk)不带的删掉.(/.) (正则
            when {
                path.matches(withApk) -> {
                    list.removeLast()
                    list.removeLast()
                }
                path.matches(hasNoApk) -> {
                    list.removeLast()
                }
                else -> notNormalPath()
            }//转换成为String 后面有/所以drop掉
            return StringBuilder().apply {
                list.forEach {
                    append(it)
                    append("/")
                }
            }.toString().dropLast(1)
        }
    }


}