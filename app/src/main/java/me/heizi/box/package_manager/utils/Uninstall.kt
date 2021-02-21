package me.heizi.box.package_manager.utils

import kotlinx.coroutines.*
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.kotlinx.shell.CommandResult
import me.heizi.kotlinx.shell.su
import kotlin.text.StringBuilder as CommandLine




object Uninstall {


    class NotSystemApp(app:String):IllegalArgumentException("${app}非系统应用")
    class NotNormallyPath(path:String):IllegalArgumentException("${path}非本软件可识别地址")
    /**
     * 获取一个带mount的commandLine
     *
     * @param mountString
     */
    private fun getStartCommand(mountString: String) = CommandLine(mountString+'\n')

    /**
     * 当验证通过时添加修改权限指令在此
     *
     * @param sDir source dir , witch path that apk locate .
     * @param packageName
     */
    private fun CommandLine.qualifiedPath(sDir: String, packageName: String) {

        //如果/data直接报错不适合
        if (sDir.startsWith("/data")) throw NotSystemApp(packageName)
        //非常规
        if (!sDir.matches(PathFormatter.withApk)) throw NotNormallyPath(sDir)
        //添加权限
        line { "chmod 777 $sDir" }
    }

    /**
     * 对着已通过的[sDir]移动达到备份效果
     *
     * @param sDir source dir
     * @param bPath backup prefix path
     */
    private fun CommandLine.movePath(sDir: String, bPath:String) {
        if (sDir.matches(PathFormatter.withApk))  {
            val l = sDir.split("/")
            val short = l.takeLast(2).joinToString("/")
            val dir = l[l.lastIndex-1]
            line { "mkdir ${bPath}/$dir" }
            line { "mv -f $sDir ${bPath}/$short" }
        }
        else throw NotNormallyPath(sDir)
    }

    /**
     * 重命名apk达到备份的效果
     *
     * @param sDir see [qualifiedPath]
     */
    private fun CommandLine.namePath(sDir: String) {
        if (!sDir.matches(PathFormatter.withApk)) throw NotNormallyPath(sDir)
        line { "mv $sDir $sDir.bak" }
    }

    /**
     * 清空数据再删除。
     *
     * @param packageName
     * @param sDir
     */
    private fun CommandLine.justRemove(packageName: String, sDir: String) {
        line { "pm clear $packageName " }
        line { "rm -rf $sDir" }
    }

    /**
     * Add uninstall command
     *
     * @param backupType
     * @param sDir
     * @param packageName
     * @return
     */
    private fun CommandLine.addUninstallCommand (
            backupType: BackupType,
            sDir:String,
            packageName: String,
    ): CommandLine {
        //判断是否合格
        qualifiedPath(sDir,packageName)
        //判断备份方式
        when(backupType) {
            is BackupType.BackupWithPath -> movePath(sDir,backupType.path)
            is BackupType.BackupWithOutPath -> namePath(sDir)
            is BackupType.JustRemove-> justRemove(packageName, sDir)
        }
        return this
    }


    private fun CommandLine.addUninstallCommand (
            backupType: BackupType,
            list:List<UninstallInfo>,
            isContinue:(Exception)->Boolean
    ): CommandLine {
        fun UninstallInfo.afterQualified():Exception? = try {
            qualifiedPath(sourceDirectory, packageName)
            null
        } catch (e:Exception) {e}
        //如果需要备份判断是否为备份
        when(backupType) {
            is BackupType.BackupWithPath -> for (i in list) {
                if(i.afterQualified()?.let(isContinue) == false) break
                movePath(i.sourceDirectory,backupType.path)
            }
            is BackupType.BackupWithOutPath -> for (i in list) { //重命名备份
                if(i.afterQualified()?.let(isContinue) == false) break
                namePath(i.sourceDirectory)
            }
            is BackupType.JustRemove->  for (i in list){ //无需备份
                if(i.afterQualified()?.let(isContinue) == false) break
                justRemove(i.packageName,i.sourceDirectory)
            }
        }
        return this
    }
//fun getUninstallCommand(mountString: String,backupType: BackupType,sDir: String,packageName:String) = getStartCommand(mountString)
//        .addUninstallCommand(backupType, sDir,packageName)
//        .toString()
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
            mountString: String
    ) = async (Dispatchers.IO) {

        //添加指令
        val command = getStartCommand(mountString)
                .addUninstallCommand(backupType, sDir,packageName)
                .toString()
        //执行
        val result = su(command)

        val record = UninstallRecord (
                name = name,
                packageName = packageName,
                source =  sDir,
                isBackups = backupType !is BackupType.JustRemove
        )
        val r = result.await()
        if (r is CommandResult.Success) {
            updateDB { record.add() }
        }
        r
    }

    suspend fun uninstall(info: UninstallInfo, backupType: BackupType, mountString: String): Deferred<CommandResult> = coroutineScope {
        uninstall(
                backupType = backupType,
                mountString = mountString,
                packageName = info.packageName,
                name = info.applicationName,
                sDir = info.sourceDirectory,
        )
    }

//    fun CoroutineScope.uninstallAll(
//            info:List<UninstallInfo>,
//            backupType: BackupType,
//            mountString: String,
//    ) = shell(
//            commandLines = getStartCommand(mountString)
//                    .addUninstallCommand(backupType,info) {true}
//                    .toString()
//                    .split('\n')
//                    .toTypedArray(),
//            isEcho = true,
//            isMixingMessage = true
//    )
}