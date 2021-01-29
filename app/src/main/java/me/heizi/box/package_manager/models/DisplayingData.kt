package me.heizi.box.package_manager.models

import android.graphics.drawable.Drawable

sealed class DisplayingData {

    data class Header(
        val path:String = "加载失败"
    ):DisplayingData()

    data class DisplayingApp(
        val appName:String = "加载失败",
        val icon: Drawable,
        val sourceDir:String,
    ):DisplayingData()
}