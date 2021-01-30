package me.heizi.box.package_manager

import dagger.hilt.android.HiltAndroidApp
import android.app.Application as App

@HiltAndroidApp
class Application: App() {
    companion object {
        const val TAG = "HeiziTool-Uninstaller"
        const val DEFAULT_MOUNT_STRING = "mount -wo remount / \n chmod 777 /"
    }
}