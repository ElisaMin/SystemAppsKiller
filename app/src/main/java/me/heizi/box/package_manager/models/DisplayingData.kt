package me.heizi.box.package_manager.models

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.diffPreviousPathAreNotSame
import java.util.*

/**
 * 在屏幕内显示的数据,由[PackageInfo]翻译而来,由[AppsPagingSource]发出.
 *
 *
 */
private sealed class DisplayingData {

    companion object {
        fun ApplicationInfo.convert(pm: PackageManager, position: Int)
                = App(
                icon = pm.getApplicationIcon(this),
                name = pm.getApplicationLabel(this).toString(),
                sDir = sourceDir ,
                position = position
        )
        suspend fun List<ApplicationInfo>.convert(pm: PackageManager,paging:Int = -1):MutableList<DisplayingData> {
            //进入循环前准备
            val time = System.currentTimeMillis()
            val result = LinkedList<DisplayingData>()
            //获取当前起始的application
            var thatApplicationInfo = get(0)
            //获取当前的前路径
            var now = PackageRepository.getPreviousPath(thatApplicationInfo.sourceDir)
            //一定存在的标题
            result.add(Header(now))
            //判断分页
            val end = paging.takeIf {it>1} ?: size
            for ( i in 0..end) {
                //添加本应用
                val app = thatApplicationInfo.convert(pm,i)
                result.add(app)
                //获取下一个的前路径 如果到底了就直接跳过
                if (i<lastIndex) {
                    thatApplicationInfo = get(i+1)
                    val next = PackageRepository.getPreviousPath(thatApplicationInfo.sourceDir)
                    //对比
                    val isNotSameShortPath = now.diffPreviousPathAreNotSame(next)
                    //前路径不等添加标题
                    if (isNotSameShortPath) result.add(Header(next))
                    //给下一次用
                    now=next
                }
            }
            Log.i(TAG, "convert: ${System.currentTimeMillis() - time}")
            return result
        }
    }

    /**
     * Header 标题
     */
    data class Header(
        val path:String = "加载失败"
    ):DisplayingData()


    data class App(
            val name: String = "加载失败",
            val icon: Drawable,
            val sDir:String,
            val position:Int
    ):DisplayingData()
}