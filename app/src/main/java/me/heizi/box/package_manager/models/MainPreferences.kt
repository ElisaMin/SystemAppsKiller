package me.heizi.box.package_manager.models

import me.heizi.kotlinx.android.preferences.PreferencesManager


/**
 * Preferences mapper Preference 管理者
 */
class MainPreferences: PreferencesManager.Global() {
//
    /** 挂载system用到的指令 */
    var mountString:String? by Named("mount_system_string")
    /** 是否备份 */
    var isBackup:Boolean? by Named("backup")
    /**
     * 备份在哪
     *
     * 空时默认值在/sdcard/android/.........
     */
    var backupPath:String? by Named("backup_path")

}