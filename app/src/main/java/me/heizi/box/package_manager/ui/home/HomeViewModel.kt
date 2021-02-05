package me.heizi.box.package_manager.ui.home

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.isUserApp
import me.heizi.box.package_manager.utils.longToast
import me.heizi.box.package_manager.utils.set
import me.heizi.box.package_manager.utils.uninstallByShell
import me.heizi.kotlinx.shell.CommandResult


class HomeViewModel(application: Application) : AndroidViewModel(application) {


    private lateinit var mapper:PreferencesMapper
    private lateinit var repository: PackageRepository
    private val _processing = MutableStateFlow(true)
    private val _uninstallStatues = MutableSharedFlow<UninstallStatues>()
    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            Log.i(TAG, "onChanged: list")
            _processing set false
        }
    }

    val processing get() = _processing.asStateFlow()
    val uninstallStatues get() = _uninstallStatues.asSharedFlow()
    val adapter by lazy {
        Adapter(
            application.packageManager,
            viewModelScope,
            repository.systemAppsFlow,
            ::uninstall
        ).apply {
            this.registerAdapterDataObserver(adapterDataObserver)
        }
    }



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
     * 开始收集
     */
    private fun starts() = viewModelScope.launch(Dispatchers.Unconfined) {
        _processing.emit(false)
        repository.systemAppsFlow.collectLatest {
            _processing.emit(true)
            launch(Main) {
                adapter.notifyDataSetChanged()
                _processing.emit(false)
            }
        }
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
     * @param position
     */
    // TODO: 2021/2/5 模拟
    private fun uninstall(position: Int) {
        _processing set true
        viewModelScope.launch(IO){
            delay(1000)
            adapter.removeAt(position)
//            _uninstallStatues.emit(UninstallStatues.Failed(CommandResult.Failed("123","456",13)))
            _processing set false
        }
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
                is CommandResult.Success -> {
                    updateDB {
                        record.add()
                    }
                    UninstallStatues.Success
                }
                is CommandResult.Failed -> {
                    UninstallStatues.Failed(r)
                }
            }.let {
                _uninstallStatues.emit(it)
            }
        }else {
            app.applicationContext.longToast("下次再添加卸载普通应用的功能哈哈")
        }
    }

    /**
     * 一个常见的状态SealedClass
     */
    sealed class UninstallStatues {
        object Success : UninstallStatues()
        class Failed(val result: CommandResult.Failed) : UninstallStatues()
    }

}