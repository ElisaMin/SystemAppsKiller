package me.heizi.box.package_manager.utils

inline fun StringBuilder.line(crossinline block:()->String) { appendLine(block()) }
object Ungroupable {
//    /**
//     * 模糊搜索
//     *
//     * todo 降低功耗
//     * @param key
//     * @param path
//     * @param name
//     */
//    fun match(key:String,path: String,name: String):Boolean {
//        if (path == key||name==key) return true
//        val keys = key.trim().toLowerCase(Locale.CHINA).split(" ","\n","/")
//        val names = name.trim().toLowerCase(Locale.CHINA).split(" ")
//        for (n in names) for (k in keys) if (n.contains(k) ) return true
//        val paths = path.trim().toLowerCase(Locale.CHINA).split(".","/","_","-")
//        for (p in paths) for (k in keys) if (p.contains(k)) return true
//        return false
//    }
}