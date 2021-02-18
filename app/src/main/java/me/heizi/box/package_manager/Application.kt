package me.heizi.box.package_manager

import android.app.Activity
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import me.heizi.box.package_manager.dao.DB
import me.heizi.box.package_manager.models.PreferencesMapper
import android.app.Application as App


class Application: App() {
    companion object {
        const val TAG = "HeiziTool-Uninstaller"
        const val DEFAULT_MOUNT_STRING = "mount -o rw,remount / \nchmod 777 /"
        const val PREFERENCES = "UNINSTALLER_CONFIG"
        var defaultBackupPath:String? = null
        const val PACKAGE_NAME="me.heizi.box.uninstaller"
        val AndroidViewModel.app get() = getApplication<Application>()
        val Activity.app get() = application as Application
    }

    val preferenceMapper by lazy { PreferencesMapper(getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)) }

    override fun onCreate() {
        super.onCreate()
        //实例化Database
        DB.resign(this)
        preferenceMapper.onStart()
        defaultBackupPath = applicationContext.getExternalFilesDir("backup")?.absolutePath
    }
}