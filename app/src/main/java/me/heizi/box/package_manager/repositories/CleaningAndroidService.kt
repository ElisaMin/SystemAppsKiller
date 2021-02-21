package me.heizi.box.package_manager.repositories


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.PACKAGE_NAME
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.Uninstall.uninstall
import me.heizi.box.package_manager.utils.io
import me.heizi.box.package_manager.utils.main
import me.heizi.kotlinx.shell.CommandResult

/**
 * 当启动时从binder里头拿数据
 *
 * @constructor Create empty Cleaning android service
 */
class CleaningAndroidService : LifecycleService() {

    private val binder by lazy { Binder() }
    private val notificationManager get() =  getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val notifier by lazy { Notifier(notificationManager, this) }
    private val context get() = this

    override fun onCreate() {
        super.onCreate()
        //build channel
        NotificationChannel(PROCESS_NOTIFY_CHANNEL_ID,"正在卸载",NotificationManager.IMPORTANCE_MIN).let(notificationManager::createNotificationChannel)
        NotificationChannel(FAILED_NOTIFY_CHANNEL_ID,"卸载失败",NotificationManager.IMPORTANCE_DEFAULT).let(notificationManager::createNotificationChannel)
        NotificationChannel(DONE_NOTIFY_CHANNEL_ID,"卸载通知",NotificationManager.IMPORTANCE_MAX).let(notificationManager::createNotificationChannel)
        //start foreground
        startForeground(PROCESS_NOTIFY_ID,notifier.processNotification.build())
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
    fun onStart(uninstallQueue:List<UninstallInfo>,backup: BackupType,mountString: String) = io {
        val task = MutableSharedFlow<Pair<UninstallInfo,Deferred<CommandResult>>>()
        val result = MutableSharedFlow<Pair<UninstallInfo,CommandResult>>()
        val failed = ArrayList<CommandResult.Failed>()
        val lastIndex = uninstallQueue.lastIndex
        launch(Unconfined) {
            var times = 0
            task.collect {
                result.emit(it.first to it.second.await())
                times++
                if (times>lastIndex) cancel()
            }
        }
        launch(Default) {
            var times = 0
            Log.i(TAG, "onStart: ${uninstallQueue.size}")
            result.collect {(i,r)->
                when(r) {
                    is CommandResult.Success -> notify(statues = true,appName = i.applicationName)
                    is CommandResult.Failed -> {
                        failed.add(r)
                        notify(false, appName = i.applicationName, reason = r.errorMessage, id = times)
                    }
                }
                Log.i(TAG, "onStart: $times")
                times++
                if (times>lastIndex) cancel()
            }
            Log.i(TAG, "onStart: cancel")
        }.invokeOnCompletion {
            launch(Main) {
                Log.i(TAG, "onStart: done")
                notifier.done(applicationContext,failed)
                binder.onDone()
            }
        }
        launch(Main) {
            notifier.start()
        }
        for (i in uninstallQueue) try {
//            if (DEBUG) task.emit(i to async{
//                delay(300)
//                CommandResult.Failed("message", null, 0)
//            }) else
            task.emit(i to uninstall(i,backup,mountString))
        } catch (e:Exception) {
            notify(false,appName = i.applicationName,reason = "${e.javaClass.simpleName}:${e.message}")
        }
    }

    private fun notify(statues:Boolean? = null,appName:String? = null,reason:String? = null,id:Int = -1) = main {
        when(statues) {
            true -> notifier.success(appName!!)
            false -> notifier.failed(appName = appName!!,reason = reason,id = id)
        }
    }


    companion object {
        fun intent(context: Context) = Intent(context, CleaningAndroidService::class.java).apply {
            action = Intent.ACTION_USER_FOREGROUND
        }
//        private const val DEBUG = true
        const val PROCESS_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.PROCESSING"
        const val FAILED_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.OPS_UNINSTALL_FAILED"
        const val DONE_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.DONE"
        const val PROCESS_NOTIFY_ID = 20
        const val DONE_NOTIFY_ID = 20
        const val FAILED_TAG = "卸载失败"
        const val EXTRA_FAILED_LIST = "$PACKAGE_NAME.FAILED_LIST"
    }

    inner class Binder:android.os.Binder() {
        lateinit var onDone: () -> Unit
        fun startUninstall(uninstallInfo: List<UninstallInfo>,backup: BackupType,mountString: String,onDone:()->Unit) {
            onStart(uninstallInfo,backup,mountString)
            this.onDone = onDone
        }
    }
}
