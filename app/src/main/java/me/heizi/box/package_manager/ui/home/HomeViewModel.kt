package me.heizi.box.package_manager.ui.home

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.isUserApp
import me.heizi.box.package_manager.utils.uninstallByShell
import me.heizi.kotlinx.shell.CommandResult


class HomeViewModel(application: Application) : AndroidViewModel(application) {


    private lateinit var mapper:PreferencesMapper

    private lateinit var repository: PackageRepository

    val adapter = NewAdapter(
            application.packageManager,
            viewModelScope,
            repository.systemAppsFlow,
            ::uninstall
    )

    /**
     * 次构造函数
     *
     * @param repository
     * @param mapper
     */
    fun start(repository: PackageRepository,mapper: PreferencesMapper) {
        this.repository = repository
        this.mapper = mapper
        starts()
    }




    /**
     * Starts
     *
     * 开始收集展示pager的东西
     */
    private fun starts() = viewModelScope.launch(Dispatchers.Unconfined) {
        TODO()
    }



    /**
     * 是否需要备份
     * @return 空为不需要 如果需要Backup的话就放个前path 就是/sdcard/abc/{apk}
     */
    private fun getBackupInfo():String?{
        // TODO: 2021/2/3 可选择式选择第三者方法也就是把APK改成APK.bak
        return if (mapper.isBackup == true)
            app.applicationContext.getExternalFilesDir("backup")!!.path
        else null
    }


    /**
     * 被界面通知到了调用的卸载
     *
     * @param data
     * @param position
     */
    // FIXME: 2021/2/3 remove item不行
    private fun uninstall(data:DisplayingData.App,position: Int) {

    }

    /**
     * 被界面通知到了调用的卸载
     *
     * @param position
     */
    // FIXME: 2021/2/3 remove item不行
    private fun uninstall(position: Int) {

    }

    /**
     * Get data path
     *
     * 判断data path是否需要删除
     * @param path
     * @return 空时不需要删除 有就删
     */
    // TODO: 2021/2/3 完成
    private fun getDataPath(path: String):String? {
        return null
    }

    private fun uninstall(applicationInfo: ApplicationInfo,appName: String) =viewModelScope.launch(IO){
        //判断是否为系统应用
        val isSystemApp = !applicationInfo.isUserApp
        //判断是否需要备份
        val backupString = getBackupInfo()
        //开始卸载
        if (isSystemApp) {
            val isBackup = backupString!=null
            val data = getDataPath(applicationInfo.dataDir)
            val result = uninstallByShell(
                    sourceDirectory = applicationInfo.sourceDir,
                    dataDirectory = data,
                    backupPath = backupString,
                    mountString = mapper.mountString!!
            )
            val record = UninstallRecord(
                    name = appName,
                    packageName = applicationInfo.packageName,
                    source =  applicationInfo.sourceDir,
                    data = data,
                    isBackups = isBackup
            )
            //卸载完成
            when(val r = result.await()) {
                is CommandResult.Success -> onUninstallSuccess(r,record)
                is CommandResult.Failed -> onUninstallFailed(r)
            }
        }else {
            // TODO: 2021/2/3 卸载普通应用
        }
    }

    /**
     * 当卸载成功时:
     *
     * @param result
     * @param record
     */
    private fun onUninstallSuccess(result: CommandResult.Success,record: UninstallRecord) {

    }

    /**
     * 当卸载失败时:
     *
     * 通知界面,展示[result]的错误.
     */
    private fun onUninstallFailed(result:CommandResult.Failed){

    }







}