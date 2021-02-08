package me.heizi.box.package_manager.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.BackupType
import me.heizi.kotlinx.shell.CommandResult
import me.heizi.kotlinx.shell.su

/**
 * Uninstall
 *
 * 卸载成为静态方法拿出来了
 * @param backupType 备份的类型
 * @param packageName 用于记录
 * @param name 用于记录
 * @param sDir apk地址
 * @param dDir data地址 当成功时会被删除
 * @param mountString 挂载指令
 */
fun CoroutineScope.uninstall(
        backupType: BackupType,
        packageName:String,
        name:String,
        sDir:String,
        dDir:String?,
        mountString: String
) = async (Dispatchers.IO) {
    //准备工作
    val sb = StringBuilder()
    fun error(): Nothing = throw IllegalArgumentException("not normally path")
    fun line(block:()->String) { sb.appendLine(block()) }
    //挂载
    line { mountString }
    //添加权限
    line { "chmod 777 $sDir" }
    //如果需要备份判断是否为备份
    when(backupType) {
        is BackupType.BackupWithPath -> { // 移动备份
            when {
                sDir.matches(PathFormatter.withApk) -> {
                    val l = sDir.split("/")
                    val short = l.takeLast(2).joinToString("/")
                    val dir = l[l.lastIndex-1]
                    line { "mkdir ${backupType.path}/$dir" }
                    line { "mv -f $sDir ${backupType.path}/$short" }
                }
                else -> error()
            }
        }
        is BackupType.BackupWithOutPath -> { //重命名备份
            if (!sDir.matches(PathFormatter.withApk)) error()
            line { "mv $sDir $sDir.bak" }
        }
        is BackupType.JustRemove->  { //无需备份
        line { "rm -rf $sDir" }
    }
    }
    val result = su(sb.toString())

    val record = UninstallRecord (
            name = name,
            packageName = packageName,
            source =  sDir,
            data = dDir,
            isBackups = backupType !is BackupType.JustRemove
    )
    val r = result.await()
    if (r is CommandResult.Success) {
        updateDB { record.add() }
        dDir?.let { su("rm -f $it") }
    }
    r
}