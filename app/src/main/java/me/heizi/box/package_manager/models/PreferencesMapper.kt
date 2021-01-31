package me.heizi.box.package_manager.models

import android.app.Activity
import android.content.Context
import me.heizi.box.package_manager.utils.PreferenceMapped


/**
 * Preferences mapper Preference 管理者
 */
class PreferencesMapper(activity:Activity):PreferenceMapped(activity.getPreferences(Context.MODE_PRIVATE)) {

    /** 挂载system用到的指令 */
    var mountString:String? by named("mount_system_string")
}