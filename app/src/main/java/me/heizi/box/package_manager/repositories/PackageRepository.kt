package me.heizi.box.package_manager.repositories

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.heizi.box.package_manager.Application

/**
 * Package repository 不按照常理出牌的Repository
 *
 * @property pm
 * @constructor
 */
class PackageRepository(
    private val pm:PackageManager
) {
//    private val systemPackages get() =  pm.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY)
    private val systemApps get()  = pm.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)

    // TODO: 2021/2/1 更新算法
    private val sortedSystemApps get() = systemApps.apply { sortBy { it.sourceDir } }
    private val _systemAppsFlow by lazy { MutableStateFlow(sortedSystemApps) }
    val systemAppsFlow get() = _systemAppsFlow.asStateFlow()

    companion object {
        val withApk by lazy { """(/[^/]+)+(/[^/]+\.apk)""".toRegex() }
        val hasNoApk by lazy { """[/\w+]+""".toRegex() }
        private val paths by lazy { """/([^/]+)+""".toRegex() }

//        @JvmStatic
//        fun main(args: Array<String>) {
//            val s = "/mnt/c/Windows_d-/system32.apk"
//            val findAll = paths.findAll(s)
//            println(findAll.count())
//            findAll.forEach{
//                println(it.value)
//            }
//        }

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