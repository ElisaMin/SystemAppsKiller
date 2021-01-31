package me.heizi.box.package_manager.models

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * 在屏幕内显示的数据,由[PackageInfo]翻译而来,由[AppsPagingSource]发出.
 *
 *
 */
sealed class DisplayingData {

    companion object {
        fun ApplicationInfo.displaying(pm: PackageManager, position: Int)
                = DisplayingApp(
                icon = pm.getApplicationIcon(this),
                name = pm.getApplicationLabel(this),
                sDir = packageName ,
                position = position
        )
    }

    /**
     * Header 标题
     */
    data class Header(
        val path:String = "加载失败"
    ):DisplayingData()


    data class DisplayingApp(
            val name: CharSequence = "加载失败",
            val icon: Drawable,
            val sDir:String,
            val position:Int
    ):DisplayingData()
}