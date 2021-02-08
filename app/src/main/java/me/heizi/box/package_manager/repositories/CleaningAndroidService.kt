package me.heizi.box.package_manager.repositories


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.uninstall
import me.heizi.kotlinx.shell.CommandResult

class CleaningAndroidService : Service() {


    private val binder by lazy { Binder() }

    class Binder:android.os.Binder() {

    }
    private val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val uninstallResults by lazy { MutableSharedFlow<Pair<UninstallInfo,CommandResult>>() }

    companion object {
        const val PROCESS_NOTIFY_CHANNEL_ID = "PROCESSING"
        const val FAILED_NOTIFY_CHANNEL_ID = "OPS_UNINSTALL_FAILED"
    }

    private val icon by lazy { Icon.createWithResource(applicationContext,R.drawable.ic_outline_notification_72) }

    private val processNotification get() = Notification
            .Builder(applicationContext, PROCESS_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setSmallIcon(icon)
            .setContentTitle("正在卸载")


    fun buildChannel() {
        NotificationChannel(PROCESS_NOTIFY_CHANNEL_ID,"正在卸载",NotificationManager.IMPORTANCE_MIN).let(notificationManager::createNotificationChannel)
        NotificationChannel(FAILED_NOTIFY_CHANNEL_ID,"卸载失败",NotificationManager.IMPORTANCE_DEFAULT).let(notificationManager::createNotificationChannel)
    }

    /**
     * On removing
     *
     * 一共三个任务同时进行,emit\uninstall\awaiting,三个都可以整一个独立的线程吧
     * @param list
     * @param backup
     * @param mountString
     */
    // TODO: 2021/2/8 增加失败超过三个自动停止的功能
    suspend fun onRemoving(list: List<UninstallInfo>, backup: BackupType, mountString: String) = coroutineScope {
        fun getTask(i: UninstallInfo) = uninstall(backupType = backup, packageName = i.packageName, name = i.applicationName, sDir = i.sourceDirectory, dDir = i.dataDirectory, mountString = mountString)
        launch(Unconfined) {
            flow {
                for (i in list) emit(i to getTask(i))
            }.flowOn(Unconfined).collect {
                val result = it.second.await()
                launch(Unconfined) {
                    uninstallResults.emit(it.first to result)
                }
            }
        }
    }


    override fun onBind(intent: Intent): IBinder  = binder
}