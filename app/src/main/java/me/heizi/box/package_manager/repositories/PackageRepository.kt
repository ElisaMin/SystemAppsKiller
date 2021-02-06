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
import kotlinx.coroutines.flow.*
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.ui.home.AdapterService
import me.heizi.box.package_manager.utils.PathFormatter.getPreviousPath
import me.heizi.box.package_manager.utils.PathFormatter.withApk
import me.heizi.box.package_manager.utils.longToast
import me.heizi.kotlinx.shell.CommandResult
import me.heizi.kotlinx.shell.su
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
    private val mapper: PreferencesMapper,
    context: Context
) {

    val systemAppsFlow get() = _systemAppsFlow.asStateFlow()
    /**
     * Key:PackageName Value:PrevPath
     */
    val prevPathIndexed:Map<String,String> get() = _prevPathIndexed
    /**
     * Key:PackageName Value:Labels
     */
    val labels:Map<String,String> get() = _labels


    private val backupPaths = context.getExternalFilesDir("backup")
    private val pm:PackageManager = context.packageManager
    private val systemApps get()  = pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
    private val _systemAppsFlow by lazy { MutableStateFlow(systemApps) }
    private val _prevPathIndexed = HashMap<String,String>()
    private val _labels = HashMap<String,String>()
    private val _uninstallStatues = MutableSharedFlow<UninstallStatues>()

    /**
     * 卸载状态
     */
    val uninstallStatues get() = _uninstallStatues.asSharedFlow()
    init {
        //获取package label
        scope.launch(Default) {
            _systemAppsFlow.value.forEach {
                _labels[it.packageName] =  pm.getApplicationLabel(it).toString()
            }
        }
        scope.launch(IO) {
            val time = _systemAppsFlow.value.sort().await()
            val all = _systemAppsFlow.value.size
            val score = (((time*2).toFloat()/all)*100).roundToInt()
            launch(Main) {
                context.longToast("加载完成，本次加载花费${time}ms。累赘指数$score。")
            }
        }
    }


    val defaultAdapterService = object:AdapterService {
        override val allAppF: StateFlow<MutableList<ApplicationInfo>>
            get() = systemAppsFlow
        override fun getAppLabel(applicationInfo: ApplicationInfo): String
            = getApplicationLabel(applicationInfo)
        override fun getAppIcon(applicationInfo: ApplicationInfo): Drawable
            = pm.getApplicationIcon(applicationInfo)
        override fun getPrevPath(applicationInfo: ApplicationInfo): String
            = prevPathIndexed.getOrDefault(applicationInfo.packageName,"分组错误")
        override fun uninstall(applicationInfo: ApplicationInfo, position: Int) {
            val isBackup = mapper.isBackup ?: true
            val task = scope.uninstall(
                isBackup  = isBackup,
                backupPath = mapper.backupPath ?: backupPaths?.absolutePath,
                sDir = applicationInfo.sourceDir,
                dDir = getDataPath(applicationInfo,isBackup),
                name = getApplicationLabel(applicationInfo),
                packageName = applicationInfo.packageName,
                mountString = mapper.mountString ?: DEFAULT_MOUNT_STRING
            )
            scope.launch(IO) {
                when (val r = task.await()) {
                    is CommandResult.Success -> UninstallStatues.Success(position)
                    is CommandResult.Failed -> UninstallStatues.Failed(r)
                }.let {
                    _uninstallStatues.emit(it)
                }
            }
        }
    }

    private fun getApplicationLabel(applicationInfo: ApplicationInfo) = labels[applicationInfo.packageName] ?: pm.getApplicationLabel(applicationInfo).toString()

    @SuppressLint("SdCardPath")
    fun getDataPath(applicationInfo: ApplicationInfo, isBackup:Boolean?):String? {
        return if (isBackup==true) {
            applicationInfo.dataDir.takeIf { it!="/data/user_de/0/"||it!="/data/user/0/" }
        } else null
    }

    /**
     * 一个常见的状态SealedClass
     */
    sealed class UninstallStatues {
        class Success(val position:Int) : UninstallStatues()
        class Failed(val result: CommandResult.Failed) : UninstallStatues()
    }



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
        val waiting = Array(group.size){false}
        val sortTask = async(IO) {
            var i = 0
            //排序子组
            group.forEach {
                launch(Default) {
                    val j = i++
                    it.value.sortBy { it.sourceDir }
                    group[it.key] = it.value
                    waiting[j] = true
                    Log.i(TAG, "sort: ${ System.currentTimeMillis() - time }")
                }
            }
            if (waiting.contains(false)) delay(1)
        }
        launch(Main){ Log.i(TAG, "sort: ${group.keys.joinToString(",")}") }
        val result = LinkedList<ApplicationInfo>()
        //等待子组排序完毕
        sortTask.await()
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
    companion object {



        val ApplicationInfo.isUserApp
            get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)


        /**
         * Uninstall
         *
         * 卸载成为静态方法拿出来了
         * @param isBackup 是否需要备份
         * @param backupPath 当空的时候会采用 apk -> apk.bak的形式 否则当开启时会移动到某个文件夹
         * @param packageName 记录
         * @param name 记录
         * @param sDir apk地址
         * @param dDir data地址 当成功时会被删除
         * @param mountString 挂载指令
         */
        fun CoroutineScope.uninstall(
            isBackup: Boolean,
            backupPath:String?,
            packageName:String,
            name:String,
            sDir:String,
            dDir:String?,
            mountString: String
        ) = async (IO) {
            //准备工作
            val sb = StringBuilder()
            fun error(): Nothing = throw IllegalArgumentException("not normally path")
            fun line(block:()->String) { sb.appendLine(block()) }
            //挂载
            line { mountString }
            //添加权限
            line { "chmod 777 $sDir" }
            //如果需要备份判断是否为备份
            when {
                isBackup && !backupPath.isNullOrEmpty() -> { // 移动备份
                    when {
                        sDir.matches(withApk) -> {
                            val l = sDir.split("/")
                            val short = l.takeLast(2).joinToString("/")
                            val dir = l[l.lastIndex-1]
                            line { "mkdir $backupPath/$dir" }
                            line { "mv -f $sDir $backupPath/$short" }
                        }
                        else -> error()
                    }
                } isBackup -> { //重命名备份
                    if (!sDir.matches(withApk)) error()
                    line { "mv $sDir $sDir.bak" }
                } else ->  { //无需备份
                    line { "rm -rf $sDir" }
                }
            }
            val result = su(sb.toString())

            val record = UninstallRecord (
                name = name,
                packageName = packageName,
                source =  sDir,
                data = dDir,
                isBackups = isBackup
            )
            val r = result.await()
            if (r is CommandResult.Success) {
                updateDB { record.add() }
                dDir?.let { su("rm -f $it") }
            }
            r
        }



    }
}