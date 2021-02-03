package me.heizi.box.package_manager

import me.heizi.box.package_manager.dao.DB
import android.app.Application as App


class Application: App() {
    companion object {
        const val TAG = "HeiziTool-Uninstaller"
        const val DEFAULT_MOUNT_STRING = "mount -o rw,remount / \nchmod 777 /"
    }

    override fun onCreate() {
        super.onCreate()
        //实例化Database
        DB.resign(this)
    }
}