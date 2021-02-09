package me.heizi.box.package_manager.repositories


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.heizi.box.package_manager.Application.Companion.PACKAGE_NAME
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.JsonContent
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.uninstall
import me.heizi.kotlinx.shell.CommandResult

class CleaningAndroidService : Service() {


    private val binder by lazy { Binder() }

    inner class Binder:android.os.Binder() {
        fun startUninstall(scope: CoroutineScope,uninstallInfo: JsonContent,backup: BackupType,mountString: String) {
            scope.launch(IO) {
                onRemoving(uninstallInfo.apps,backup,mountString)
            }
        }
    }
    private val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val uninstallResults by lazy { MutableSharedFlow<Pair<UninstallInfo,CommandResult>>() }

    companion object {
        const val PROCESS_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.PROCESSING"
        const val FAILED_NOTIFY_CHANNEL_ID = "$PACKAGE_NAME.OPS_UNINSTALL_FAILED"
        const val PROCESS_NOTIFY_ID = 20
        const val PROCESS_NOTIFY_TAG = "$PACKAGE_NAME.uninstalling"
        const val FAILED_TAG = "卸载失败"

    }

    private val icon by lazy { Icon.createWithResource(applicationContext,R.drawable.ic_outline_notification_72) }

    private val processNotification get() = Notification
            .Builder(applicationContext, PROCESS_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setSmallIcon(icon)
            .setContentTitle("正在卸载")
    private val failedNotification get() = Notification
            .Builder(applicationContext, FAILED_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
            .setSmallIcon(icon)


    /**
     * Failed times 失败次数
     */
    private var failedTimes  = 0

    /**
     * 失败的通知
     *
     * @param reason 原因是啥
     * @param appName 应用名称
     */
    private suspend fun buildFailedNotification(reason:String?,appName:String) = coroutineScope {
        if (failedTimes>=6) launch(Main){
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
        } else launch(Main){
            failedNotification
                    .setContentTitle("卸载失败...")
                    .setNumber(failedTimes++)
                    .build()
                    .let {
                        notificationManager.notify(FAILED_TAG,8,it)
                    }
        }

    }

    /**
     * Show progress
     *
     * @param s
     */
    private suspend fun showProgress(s:String) = coroutineScope {
        launch(Main) {
            processNotification.setContentTitle("卸载成功....").setContentText(s).build().let {
                notificationManager.notify(PROCESS_NOTIFY_TAG, PROCESS_NOTIFY_ID,it)
            }
        }

    }

    private fun buildChannel() {
        NotificationChannel(PROCESS_NOTIFY_CHANNEL_ID,"正在卸载",NotificationManager.IMPORTANCE_MIN).let(notificationManager::createNotificationChannel)
        NotificationChannel(FAILED_NOTIFY_CHANNEL_ID,"卸载失败",NotificationManager.IMPORTANCE_DEFAULT).let(notificationManager::createNotificationChannel)
    }

    private suspend fun collectResults(pair: Pair <UninstallInfo,CommandResult>): Unit = coroutineScope {
        val (a,r) = pair
        when(r){
            is CommandResult.Success -> {
                showProgress(a.applicationName)
            }
            is CommandResult.Failed -> {
                buildFailedNotification(r.errorMessage?.takeIf { it.isNotEmpty() },a.applicationName)
            }
        }
    }

    private var _scope:CoroutineScope? = MainScope()
    private val scope = _scope!!
    override fun onCreate() {
        super.onCreate()
        buildChannel()
        scope.launch(Unconfined) {
            uninstallResults.collect(::collectResults)
        }
        startForeground(PROCESS_NOTIFY_ID,processNotification.build())
    }

    override fun onDestroy() {
        super.onDestroy()
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
