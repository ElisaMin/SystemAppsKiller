package me.heizi.box.package_manager.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PathFormatter {


    val withApk by lazy { """(/[^/]+)+(/[^/]+\.apk)""".toRegex() }
    val hasNoApk by lazy { """[/\w+]+""".toRegex() }
    private val paths by lazy { """/([^/]+)+""".toRegex() }
    /**
     * Diff previous path
     *
     * 对比[getPreviousPath] 的结果是否相等 不相等时返回true
     */
    suspend fun String.diffPreviousPathAreNotSame(prev: String):Boolean = withContext(Dispatchers.Main){
        val s = this@diffPreviousPathAreNotSame
        var notSame = (s!=prev)
        if (notSame) {
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