package me.heizi.box.package_manager.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.box.package_manager.utils.dialog

class StopForegroundService : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive: got it")
        try {
            CleaningAndroidService.intent(context).let {
                context.stopService(it)
            }
        } catch (e:Exception) {
            Log.i(TAG, "onReceive: $e")
            context.dialog(title = "学术不精导致的错误",message = e.toString())
        }
    }
}