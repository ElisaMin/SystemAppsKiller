package me.heizi.box.package_manager.models

import me.heizi.box.package_manager.Application

/**
 * Backup type
 *
 * 支持的备份方法
 */
sealed class BackupType {
    /**
     * Just remove
     *
     * /system/....apk -> GONE
     * /data/....apk -> GONE
     */
    object JustRemove:BackupType()

    /**
     * Backup without path
     *
     * apk->apk.bak
     */
    object BackupWithOutPath:BackupType()

    /**
     * Backup with path
     *
     * /system/.....apk -> /data/......apk
     */
    sealed class BackupWithPath(val path:String):BackupType() {
        object Default:BackupWithPath(Application.defaultBackupPath!!)
        class Custom(path:String):BackupWithPath(path)
    }
}