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
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.ui.home.AdapterService
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
            uninstallSystemApp(position = position,applicationInfo = applicationInfo)
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
     * Uninstall system app
     *
     * 有三种模式可以卸载
     * 暴力删除:remove
     * 移动备份:move path
     * 改名备份:mv .apk .apk.bak
     */
    fun uninstallSystemApp(
        applicationInfo: ApplicationInfo,
        position: Int
    ) = scope.launch(Default) {
        val sb = StringBuilder()
        fun error(): Nothing = throw IllegalArgumentException("not normally path")
        fun line(block:()->String) { sb.appendLine(block()) }

        //准备工作
        val isBackup  = mapper.isBackup == true
        val backupPath = mapper.backupPath ?: backupPaths?.absolutePath
        val sDir = applicationInfo.sourceDir
        val dDir = getDataPath(applicationInfo,isBackup)
        //挂载
        line { mapper.mountString ?: Application.DEFAULT_MOUNT_STRING }
        //添加权限
        line { "chmod 777 $sDir" }
        //如果需要备份判断是否为备份
        when {
            isBackup && !backupPath.isNullOrEmpty() -> { // 移动备份
                val short = when {
                    sDir.matches(withApk) -> sDir.split("/").takeLast(2).joinToString("/")
                    sDir.matches(hasNoApk) -> sDir.split("/").last()
                    else -> error()
                }
                line { "mv -f $sDir $backupPath/$short" }
            } isBackup -> { //重命名备份
                if (!sDir.matches(withApk)) error()
                line { "mv $sDir $sDir.bak" }
            } else ->  { //无需备份
                line { "rm -rf $sDir" }
            }
        }
        dDir?.let {
            line { "rm -f $it" }
        }
        val result = scope.su(sb.toString())

        val record = UninstallRecord (
            name = getApplicationLabel(applicationInfo),
            packageName = applicationInfo.packageName,
            source =  sDir,
            data = dDir,
            isBackups = isBackup
        )

        scope.launch(IO) {
            when (val r = result.await()) {
                is CommandResult.Success -> {
                    updateDB { record.add() }
                    UninstallStatues.Success(position)
                }
                is CommandResult.Failed -> UninstallStatues.Failed(r)
            }.let {
                _uninstallStatues.emit(it)
            }
        }
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



        val withApk by lazy { """(/[^/]+)+(/[^/]+\.apk)""".toRegex() }
        val hasNoApk by lazy { """[/\w+]+""".toRegex() }
        private val paths by lazy { """/([^/]+)+""".toRegex() }


        val ApplicationInfo.isUserApp
            get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)


        /**
         * Diff previous path
         *
         * 对比[getPreviousPath] 的结果是否相等 不相等时返回true
         */
        suspend fun String.diffPreviousPathAreNotSame(prev: String):Boolean = withContext(Main){
            val s = this@diffPreviousPathAreNotSame
            var notSame = (s!=prev)
            if (notSame) {
                Log.i(Application.TAG, "diffPreviousPathAreNotSame: $s $prev not same yet")

                val l1 = paths.findAll(s).map {
                    it.value.replace("/","").takeIf { it.isNotEmpty() }
                }.filter { it!=null }.toList()

                val l2 = paths.findAll(prev).map {
                    it.value.replace("/","").takeIf { it.isNotEmpty() }
                }.filter { it!=null }.toList()
                val c1 = l1.size
                val c2 = l2.size
                notSame = (c1 != c2)
                if (!notSame)
                    repeat((if (c1 < c2) c1 else c2)-1) { i->
                        if (l1[i] != l2[i]) notSame = true}
            }
            notSame
        }

        /**
         * Get previous path
         *
         * 把/system/app/any/path.apk的 /system/app/ 剪下来
         */
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