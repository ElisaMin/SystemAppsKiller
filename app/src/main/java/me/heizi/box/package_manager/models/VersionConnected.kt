package me.heizi.box.package_manager.models

import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.BackupType.Companion.HasNoBackup
import me.heizi.box.package_manager.models.BackupType.Companion.MoveDir

/**
 * Version connected 连接表后做到的效果
 *
 * 达到的效果可以看README#数据库#最终效果 篇章
 *
 * @property id
 * @property name
 * @property isBackup
 * @property createTime
 * @property apps
 */
data class VersionConnected(
    val id: Int = 0,
    override val name: String,
    val isBackup: Boolean =true,
    override val createTime: Int,
    override val apps:List<UninstallRecord>
) : CompleteVersion {
    override val backupType: Int = if(isBackup) MoveDir else HasNoBackup
}