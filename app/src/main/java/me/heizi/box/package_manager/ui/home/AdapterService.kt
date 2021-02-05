package me.heizi.box.package_manager.ui.home

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.StateFlow

interface AdapterService {
    val allAppF:StateFlow<MutableList<ApplicationInfo>>
    val allApps get() =  allAppF.value

    fun removeItemFormAllApps(applicationInfo: ApplicationInfo) = allAppF.value.remove(applicationInfo)
    fun getAppLabel(applicationInfo: ApplicationInfo):String
    fun getAppIcon(applicationInfo: ApplicationInfo):Drawable?
    fun getPrevPath(applicationInfo: ApplicationInfo):String
    fun uninstall(applicationInfo: ApplicationInfo, position: Int)
}