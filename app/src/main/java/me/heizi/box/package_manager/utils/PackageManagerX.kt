package me.heizi.box.package_manager.utils

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES

/**
 * API:
 * 随时可变 所以是SharedFlow
 * 返回一个HashMap<Key,List<PackageInfo>>
 * 还有一个通知消息改变的function
 *
 */
@SuppressLint("QueryPermissionsNeeded")
class Apps(
    val pm:PackageManager
) {

    companion object {
        private val withApk by lazy { """[/\w+]+/.+\.apk""".toRegex() }
        private val hasNoApk by lazy { """[/\w+]+""".toRegex() }

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


//    private val apps by lazy {
//        MutableStateFlow<HashMap<String,ArrayList<PackageInfo>>>(hashMapOf())
//    }
//    val asFlow:Flow<HashMap<String,ArrayList<PackageInfo>>> get() =  apps
//    val value:HashMap<String,ArrayList<PackageInfo>> get() = apps.value
//    val systemApps get() =
//        value.map {(k,v)->
//            k to v.filter {
//                !it.applicationInfo.isUserApp
//            }
//        }

    val asSortedList : List<ApplicationInfo>
        get() = getAllApps()
            .map {
                it.applicationInfo
            }
            .toMutableList()
            .also { list ->
                list.sortBy { it.sourceDir }
            }


    private fun getAllApps() = pm.getInstalledPackages(GET_ACTIVITIES)
//    suspend fun emit() {
//        val hashMap:HashMap<String,ArrayList<PackageInfo>> = hashMapOf()
//        for (p in getAllApps()) {
//            val path = getPreviousPath(p.applicationInfo.sourceDir)
//            hashMap[path]?.add(p) ?: kotlin.run { hashMap[path] = arrayListOf(p) }
//        }
//        apps.emit(hashMap)
//    }




}






val ApplicationInfo.isUserApp
    get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)