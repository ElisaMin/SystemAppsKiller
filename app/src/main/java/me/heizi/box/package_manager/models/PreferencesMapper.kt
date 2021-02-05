package me.heizi.box.package_manager.models

import android.content.SharedPreferences
import me.heizi.box.package_manager.utils.PreferenceMapped


/**
 * Preferences mapper Preference 管理者
 */
class PreferencesMapper(preferences: SharedPreferences):PreferenceMapped(preferences) {

    /** 挂载system用到的指令 */
    var mountString:String? by named("mount_system_string")
    /** 是否备份 */
    var isBackup:Boolean? by named("backup")
    /**
     * 备份在哪
     *
     * 空时默认值在/sdcard/android/.........
     */
    var backupPath:String? by named("backup_path")

}