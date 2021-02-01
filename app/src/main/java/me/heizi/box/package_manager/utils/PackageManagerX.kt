package me.heizi.box.package_manager.utils

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.async
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.hasNoApk
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.withApk
import me.heizi.kotlinx.shell.su


val ApplicationInfo.isUserApp
    get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)


fun CoroutineScope.uninstallByShell(
    sourceDirectory:String,
    dataDirectory:String?=null,
    backupPath:String?=null,
    mountString: String
) = async (Default) {
"""
$mountString
${sourceDirectory.backupOrDelete(backupPath)}
${dataDirectory.chmodAndRemoveCommand()}
""".trimIndent().let {
        su(*it.lines().toTypedArray(),isErrorNeeding = true).await()
    }
}
private fun String.backupOrDelete(backupPath: String?)=
        if (backupPath.isNullOrEmpty()) {
            chmodAndRemoveCommand()
        } else {
            val short = when {
                matches(withApk) -> {
                    split("/")
                            .takeLast(2)
                            .joinToString("/")
                }
                matches(hasNoApk) ->{
                    split("/")
                            .last()
                }
                else ->{
                    throw IllegalArgumentException("not normally path")
                }
            }
            "chmod 777 $this \n"+
            "mv -f $this $backupPath/$short"
        }

private fun String?.chmodAndRemoveCommand()=
        this?.let { path ->
            "chmod 777 $path \n" +
            "rm -rf $path \n"
        } ?:""