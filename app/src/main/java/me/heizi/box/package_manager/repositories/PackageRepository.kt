package me.heizi.box.package_manager.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.activities.home.adapters.UninstallApplicationAdapter
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.utils.PathFormatter.getPreviousPath
import me.heizi.box.package_manager.utils.Uninstall.uninstall
import me.heizi.box.package_manager.utils.longToast
import me.heizi.kotlinx.shell.CommandResult
import java.util.*
import kotlin.collections.set
import kotlin.math.roundToInt

/**
 * Package repository 不按照常理出牌的Repository
 *
 * @property pm
 * @constructor
 */
class PackageRepository(
    private val scope: CoroutineScope,
    context: Context,
    getBackupType: ()->BackupType,
    getMountString:()-> String
) {

    /**
     * 卸载状态
     */
    val uninstallStatues get() = _uninstallStatues.asSharedFlow()

    private val systemApps get()  = pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
    private val pm:PackageManager = context.packageManager
    private val _systemAppsFlow: MutableStateFlow<MutableList<ApplicationInfo>> by lazy { MutableStateFlow(mutableListOf()) }
    private val _uninstallStatues = MutableSharedFlow<UninstallStatues>()
    /**
     * Key:PackageName Value:PrevPath
     */
    private val _prevPathIndexed = HashMap<String,String>()
    /**
     * Key:PackageName Value:Labels
     */
    private val _labels = HashMap<String,String>()

    init {
        //获取package label
        scope.launch(Default) {
            _systemAppsFlow.value.forEach {
                _labels[it.packageName] =  pm.getApplicationLabel(it).toString()
            }
        }
        scope.launch(IO) {
            val time = notifyDataChanged().await()
            val all = _systemAppsFlow.value.size
            val score = (((time*2).toFloat()/all)*100).roundToInt()
            launch(Main) {
                context.longToast("加载完成，本次加载花费${time}ms。累赘指数$score。")
            }
        }
    }


    val defaultAdapterService = object: UninstallApplicationAdapter.Service {
        override val allAppF: StateFlow<MutableList<ApplicationInfo>>
            get() = _systemAppsFlow
        override fun getAppLabel(applicationInfo: ApplicationInfo): String
            = getApplicationLabel(applicationInfo)
        override fun getAppIcon(applicationInfo: ApplicationInfo): Drawable
            = pm.getApplicationIcon(applicationInfo)
        override fun getPrevPath(applicationInfo: ApplicationInfo): String
            = _prevPathIndexed.getOrDefault(applicationInfo.packageName,"分组错误")
        override fun uninstall(applicationInfo: ApplicationInfo, position: Int) {
            val backupType = getBackupType()
            try {
                val task = scope.uninstall(
                    backupType = backupType,
                    sDir = applicationInfo.sourceDir,
                    name = getApplicationLabel(applicationInfo),
                    packageName = applicationInfo.packageName,
                    mountString = getMountString()
                )
                task.invokeOnCompletion { e->
                    e?.let { scope.launch { _uninstallStatues.emit(UninstallStatues.Failed("错误:${e.javaClass.simpleName}",e.message,-1)) } }
                }
                scope.launch(IO) {
                    when (val r = task.await()) {
                        is CommandResult.Success -> UninstallStatues.Success(position)
                        is CommandResult.Failed -> UninstallStatues.Failed(processingMessage = r.processingMessage,errorMessage = r.errorMessage,r.code)
                    }.let {
                        _uninstallStatues.emit(it)
                    }
                }
            }catch (e:Exception) {
                scope.launch { _uninstallStatues.emit(UninstallStatues.Failed("错误:${e.javaClass.simpleName}",e.message,-1)) }
            }
        }
    }

    private fun getApplicationLabel(applicationInfo: ApplicationInfo) = _labels[applicationInfo.packageName] ?: pm.getApplicationLabel(applicationInfo).toString()

    @SuppressLint("SdCardPath")
    fun getDataPath(applicationInfo: ApplicationInfo,backupType: BackupType):String? =
        applicationInfo.dataDir.takeIf { backupType==BackupType.JustRemove || it!="/data/user_de/0/"||it!="/data/user/0/" }
    fun notifyDataChanged():Deferred<Long> = systemApps.sort()



    /**
     * 给path排序
     *
     * 假如有个path list为
     * /a/b/e/f.g
     * /a/b/c/d.e
     * /a/b/a/b/d.e
     * /o/p/q/r.s
     * /h/i/j/k/l.m
     * 先按照后面拆分两个path段 得到/a/b和c/d.e两个path或这h/i/j和k/l.m
     * 先排序第一段 然后再进行子path排序
     */
    private fun List<ApplicationInfo>.sort() = MainScope().async(Default){
        Log.i(TAG, "sort: start")
        //记录时间
        val time=System.currentTimeMillis()
        //用hashmap区分前面的路径
        val group = HashMap<String,ArrayList<ApplicationInfo>>()
        //循环分组 并进行索引
        forEach {
            val k = getPreviousPath(it.sourceDir)
            _prevPathIndexed[it.packageName] = k
            group[k]?.add(it) ?: kotlin.run{ group[k] = arrayListOf(it) }
        }//耗时操作
        val sortJob = launch(IO) {
            //排序子组
            group.forEach {
                launch(Default) {
                    it.value.sortBy { it.sourceDir }
                    group[it.key] = it.value
                    Log.i(TAG, "sort: ${ System.currentTimeMillis() - time }")
                }
            }
        }
        launch(Main){ Log.i(TAG, "sort: ${group.keys.joinToString(",")}") }
        val result = LinkedList<ApplicationInfo>()
        //等待子组排序完毕
        sortJob.join()
        while (sortJob.isActive) Unit
        //排序父组
        group.entries.sortedBy { it.key }.forEach {
            result.addAll(it.value)
        }
        //记录时间
        val dealTime = System.currentTimeMillis() - time
        Log.i(TAG, "sort: $dealTime")
        _systemAppsFlow.emit(result)
        System.gc()
        dealTime
    }


    /**
     * 一个常见的状态SealedClass
     */
    sealed class UninstallStatues {
        class Success(val position:Int) : UninstallStatues()
        class Failed(val processingMessage:String,val errorMessage:String?,val code:Int) : UninstallStatues()
    }
    companion object {
        val ApplicationInfo.isUserApp
            get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)
    }
}