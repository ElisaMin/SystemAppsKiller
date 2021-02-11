package me.heizi.box.package_manager.repositories


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import me.heizi.box.package_manager.Application.Companion.PACKAGE_NAME
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.broadcast.StopForegroundService
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.uninstall
import me.heizi.kotlinx.shell.CommandResult

// TODO: 2021/2/10 添加超时自杀
class CleaningAndroidService : Service() {


    private val binder by lazy { Binder() }


    inner class Binder:android.os.Binder() {
        fun startUninstall(scope: CoroutineScope,uninstallInfo: List<UninstallInfo>,backup: BackupType,mountString: String) {
            scope.launch(IO) {
                onRemoving(uninstallInfo,backup,mountString)
            }
        }
    }
    private val notificationManager get() =  baseContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    companion object {
        fun intent(context: Context) = Intent(context, CleaningAndroidService::class.java).apply {
            action = Intent.ACTION_USER_FOREGROUND
        }
        const val PROCESS_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.PROCESSING"
        const val FAILED_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.OPS_UNINSTALL_FAILED"
        const val DONE_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.DONE"
        const val PROCESS_NOTIFY_ID = 20
        const val DONE_NOTIFY_ID = 20
        const val FAILED_TAG = "卸载失败"

    }

    private val icon by lazy { Icon.createWithResource(baseContext,R.drawable.ic_outline_notification_72) }

    private val processNotification get() = Notification
            .Builder(baseContext, PROCESS_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setSmallIcon(icon)
            .setContentTitle("正在卸载")
    private val failedNotification get() = Notification
            .Builder(baseContext, FAILED_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
            .setSmallIcon(icon)


    /**
     * Failed times 失败次数
     */
    private var failedTimes  = 0

    /**
     * 通知失败
     *
     * @param reason 原因是啥
     * @param appName 应用名称
     */
    private suspend fun notifyFailed(reason:String?, appName:String) = coroutineScope {

        val style = reason?.let {
            Notification.BigTextStyle().setSummaryText(appName).bigText(reason).setBigContentTitle("卸载失败,原因:")
        }?:Notification.BigTextStyle().setSummaryText(appName).bigText("卸载失败但无错误原因")

        failedNotification
            .setContentText(reason)
            .setStyle(style)
            .build()
            .let {
                notificationManager.notify(FAILED_TAG, failedTimes++,it)
            }
    }

    /**
     * 通知成功
     *
     * @param appName
     */
    private suspend fun notifySuccess(appName:String) = coroutineScope {
        launch(Main) {
            processNotification.setContentTitle("卸载成功....").setContentText(appName).build().let {
                notificationManager.notify( PROCESS_NOTIFY_ID,it)
            }
        }
    }

    /**
     * 通知完成
     *
     * 显示停止按钮和帮助文字提示正在耗电和点击后的后果。
     */
    private suspend fun notifyDone() = coroutineScope { launch(Main) {
        val pIntent = PendingIntent.getBroadcast(this@CleaningAndroidService,0, Intent(this@CleaningAndroidService,StopForegroundService::class.java),PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this@CleaningAndroidService, DONE_NOTIFY_CHANNEL_ID)
            .setContentTitle("卸载完成")
            .setContentText("关闭前台服务时可能通知会随之消失，请根据耗电情况自行关闭本服务。")
            .addAction(R.drawable.ic_baseline_clear_24,"关闭",pIntent)
            .setSmallIcon(R.drawable.ic_outline_done_24)
            .build()
        notificationManager.notify(DONE_NOTIFY_ID,n)
    } }

    /**
     * 通知开始
     *
     */
    private suspend fun notifyStart() = coroutineScope { launch(Main) {
        val n = NotificationCompat.Builder(this@CleaningAndroidService, DONE_NOTIFY_CHANNEL_ID)
            .setContentTitle("卸载开始")
            .setSmallIcon(R.drawable.ic_outline_done_24)
            .build()
        notificationManager.notify(DONE_NOTIFY_ID,n)
    } }
    private fun buildChannel() {
        NotificationChannel(PROCESS_NOTIFY_CHANNEL_ID,"正在卸载",NotificationManager.IMPORTANCE_MIN).let(notificationManager::createNotificationChannel)
        NotificationChannel(FAILED_NOTIFY_CHANNEL_ID,"卸载失败",NotificationManager.IMPORTANCE_DEFAULT).let(notificationManager::createNotificationChannel)
        NotificationChannel(DONE_NOTIFY_CHANNEL_ID,"卸载通知",NotificationManager.IMPORTANCE_MAX).let(notificationManager::createNotificationChannel)
    }

    private suspend fun collectResults(pair: Pair <UninstallInfo,CommandResult>): Unit = coroutineScope {
        val (a,r) = pair
        when(r){
            is CommandResult.Success -> {
                notifySuccess(a.applicationName)
            }
            is CommandResult.Failed -> {
                notifyFailed(r.errorMessage?.takeIf { it.isNotEmpty() },a.applicationName)
            }
        }
    }

    private var _scope:CoroutineScope? = MainScope()
    private val scope = _scope!!
    override fun onCreate() {
        super.onCreate()
        buildChannel()
        startForeground(PROCESS_NOTIFY_ID,processNotification.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: bye~")
        scope.cancel()
        _scope = null
    }


    /**
     * On removing
     *
     * @param list
     * @param backup
     * @param mountString
     */
    suspend fun onRemoving(list: List<UninstallInfo>, backup: BackupType, mountString: String) = coroutineScope {
        fun getTask(i: UninstallInfo) = uninstall(backupType = backup, packageName = i.packageName, name = i.applicationName, sDir = i.sourceDirectory, dDir = i.dataDirectory, mountString = mountString)
        launch(Default) {
            flow { for (i in list) emit(i to getTask(i)) }
                .flowOn(IO)
                .map { it.first to it.second.await() }
                .onCompletion { notifyDone() }
                .collect(::collectResults)
        }
    }


    override fun onBind(intent: Intent): IBinder  = binder
}
