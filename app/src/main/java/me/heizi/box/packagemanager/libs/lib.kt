package me.heizi.box.packagemanager.libs

import dev.utils.app.ShellUtils


infix fun ShellUtils.CommandResult.success(doSTH:ShellUtils.CommandResult.()->Unit): ShellUtils.CommandResult {
    if (this.isSuccess){
        doSTH()
    }
    return this
}
infix fun ShellUtils.CommandResult.fall(doSTH: ShellUtils.CommandResult.() -> Unit): ShellUtils.CommandResult {
    if (!this.isSuccess){
        doSTH()
    }
    return this
}
val defaultUninstallList = ("""/oem/OP/priv-app/LGU_KR/LGAssistant
                                /system/product/priv-app/CollageWallpapers
                                /system/product/priv-app/MyPlacesEngine
                                /data/user/0/com.lge.qlens
                                /data/user/0/com.google.android.apps.docs.editors.slides
                                /data/user/0/com.google.android.youtube
                                /data/user_de/0/com.lge.myplace
                                /system/product/priv-app/LGLW_MultiPhoto/LGLW_MultiPhoto.apk
                                /data/user/0/com.lge.gallery.vr.wallpaper
                                /data/user/0/com.google.android.music
                                /system/product/priv-app/Facebook_Service/Facebook_Service.apk
                                /data/user/0/com.lge.lgassistant
                                /data/user/0/com.google.ar.lens
                                /data/user/0/com.google.android.apps.maps
                                /system/app/EditorsSlides
                                /data/user_de/0/com.lge.ime
                                /system/app/Drive
                                /data/user/0/com.lge.video.vr.wallpaper
                                /system/app/EditorsSheets
                                /data/user/0/com.facebook.appmanager
                                /system/app/Maps
                                /data/user/0/com.lge.iuc
                                /data/user/0/com.lge.vrplayer
                                /data/user/0/com.google.android.apps.docs
                                /system/product/priv-app/LGMapUI
                                /system/app/LensStub
                                /data/user/0/com.google.android.videos
                                /data/user/0/com.google.android.apps.tachyon
                                /system/product/priv-app/LGVRPlayer
                                /data/user/0/com.google.android.gm
                                /system/product/priv-app/LGLiveWallpapersPicker
                                /data/user/0/com.android.wallpaper.livepicker
                                /system/priv-app/LGEIME
                                /system/product/app/Facebook_AppManager/Facebook_AppManager.apk
                                /oem/OP/priv-app/LGU_KR/QLens
                                /data/user/0/com.lge.tdmb
                                /data/user/0/com.lge.lgpay
                                /oem/OP/priv-app/LGU_KR/ONEStoreClient
                                /system/app/Music2
                                /system/app/EditorsDocs
                                /system/product/priv-app/LGIUC
                                /oem/OP/priv-app/LGU_KR/ONEStoreService
                                /data/user/0/com.lge.lgmapui
                                /data/user_de/0/com.lge.myplace.engine
                                /system/app/Videos
                                /data/user/0/com.lge.gallery.collagewallpaper
                                /system/priv-app/LGMyPlace
                                /data/user/0/com.lguplus.appstore
                                /oem/OP/priv-app/LGU_KR/LGTDMB
                                /system/priv-app/DocumentsUI
                                /system/priv-app/LG360VideoWallpaper
                                /data/user/0/com.android.documentsui
                                /system/product/priv-app/LGPay
                                /data/user/0/com.lge.livewallpaper.multiphoto
                                /data/user/0/com.google.android.apps.docs.editors.docs
                                /data/user/0/com.lguplus.mobile.cs
                                /system/app/Gmail2
                                /data/user/0/com.facebook.services
                                /system/app/YouTube
                                /system/app/Duo
                                /data/user/0/com.google.android.apps.docs.editors.sheets
                                /system/product/priv-app/LG360Wallpaper
                                /data/user/0/com.skt.skaf.OA00018282
                                /oem/OP/app/LGU_KR/lguplusp_mcc/lguplusp_mcc.apk""".trimIndent())
class lib {

    companion object
}