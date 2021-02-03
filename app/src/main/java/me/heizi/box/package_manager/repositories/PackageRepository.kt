package me.heizi.box.package_manager.repositories

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.utils.longToast
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
    context: Context
) {
    private val pm:PackageManager = context.packageManager
    private val systemApps get()  = pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)

    private val _systemAppsFlow by lazy { MutableStateFlow(systemApps) }
    val systemAppsFlow get() = _systemAppsFlow.asStateFlow()


    init {
        scope.launch(IO) {
            val time = _systemAppsFlow.value.sort().await()
            val all = _systemAppsFlow.value.size
            val score = (((time*2).toFloat()/all)*100).roundToInt()
            launch(Main) {
                context.longToast("排序完成，本次排序花费${time}ms。累赘指数$score。")
            }

        }
        Log.i(TAG, "init: sorting")
    }

    /**
     * 对比显示中数据和ApplicationInfo是否一致
     *
     * @param data
     * @param info
     * @return 是否一致
     */
//    private fun diff(data: DisplayingData.App,info:ApplicationInfo):Boolean =
//        (data.name == pm.getApplicationLabel(info).toString() && data.sDir == info.sourceDir)
//
//


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
        //循环分组
        forEach {
            val k = getPreviousPath(it.sourceDir)
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
        dealTime
    }
    companion object {
        val withApk by lazy { """(/[^/]+)+(/[^/]+\.apk)""".toRegex() }
        val hasNoApk by lazy { """[/\w+]+""".toRegex() }
        private val paths by lazy { """/([^/]+)+""".toRegex() }


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