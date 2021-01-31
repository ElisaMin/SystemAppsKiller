package me.heizi.box.package_manager.utils

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES



val ApplicationInfo.isUserApp
    get() = (flags and ApplicationInfo.FLAG_SYSTEM <= 0)