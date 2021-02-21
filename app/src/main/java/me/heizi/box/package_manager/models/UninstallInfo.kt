package me.heizi.box.package_manager.models

interface UninstallInfo{

    val applicationName:String
    val packageName:String
    val sourceDirectory:String

    data class DefaultUninstallInfo(
        override val applicationName:String,
        override val packageName:String,
        override val sourceDirectory:String,
    ):UninstallInfo
}