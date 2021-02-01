package me.heizi.box.package_manager

import android.app.Application as App


class Application: App() {
    companion object {
        const val TAG = "HeiziTool-Uninstaller"
        const val DEFAULT_MOUNT_STRING = "mount -o rw,remount / \nchmod 777 /"
    }
}