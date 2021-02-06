package me.heizi.box.package_manager.models

data class UninstallInfo(
    val applicationName:String,
    val packageName:String,
    val sourceDirectory:String,
    val dataDirectory:String?
)