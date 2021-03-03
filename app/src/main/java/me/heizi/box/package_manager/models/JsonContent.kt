package me.heizi.box.package_manager.models

/**
 * 分享在网络世界内除了版本信息真正生效的东西
 *
 * @property name
 * @property createTime
 * @property isBackup
 * @property apps
 */
data class JsonContent(
    override val name:String,
    override val createTime:Int,
    override val backupType: Int,
    override val apps:List<UninstallInfo>
):CompleteVersion

