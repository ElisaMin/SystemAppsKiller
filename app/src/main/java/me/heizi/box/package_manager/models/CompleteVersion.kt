package me.heizi.box.package_manager.models


/**
 * 完整的应用卸载信息
 */
interface CompleteVersion {
    val name:String
    val apps:List<UninstallInfo>
    val createTime:Int
    val backupType:Int
}