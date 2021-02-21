package me.heizi.box.package_manager.repositories

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.app.NotificationCompat
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.ShowResult
import me.heizi.box.package_manager.repositories.CleaningAndroidService.Companion.EXTRA_FAILED_LIST
import me.heizi.kotlinx.shell.CommandResult

class Notifier (
    private val notificationManager: NotificationManager,
    private val context: Context
) {
    val processNotification get() = Notification.Builder(context, CleaningAndroidService.PROCESS_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setSmallIcon(icon)
            .setContentTitle("正在卸载")
    private val failedNotification get() = Notification.Builder(context, CleaningAndroidService.FAILED_NOTIFY_CHANNEL_ID)
            .setLargeIcon(icon)
            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
            .setSmallIcon(icon)
    private val icon by lazy { Icon.createWithResource(context, R.drawable.ic_outline_notification_72) }
    /**
     * 通知失败
     *
     * @param reason 原因是啥
     * @param appName 应用名称
     */
    fun failed(reason:String?, appName:String,id:Int)  {
        val style = reason?.let {
            Notification.BigTextStyle().setSummaryText(appName).bigText(reason).setBigContentTitle("卸载失败,原因:")
        }?: Notification.BigTextStyle().setSummaryText(appName).bigText("卸载失败但无错误原因")
        failedNotification
                .setContentText(reason)
                .setStyle(style)
                .build()
                .let {
                    notificationManager.notify(CleaningAndroidService.FAILED_TAG, id,it)
                }
    }
    /**
     * 通知成功
     *
     * @param appName
     */
    fun success(appName:String)  {
        Log.i(Application.TAG, "notifySuccess: called")
        processNotification.setContentTitle("卸载成功....").setContentText(appName).build().let {
            notificationManager.notify(CleaningAndroidService.PROCESS_NOTIFY_ID,it)
        }
    }
    /**
     * 通知完成
     *
     * 显示卸载完成点击查看失败列表
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    fun done(context: Context, list: ArrayList<CommandResult.Failed>) = NotificationCompat
        .Builder(context, CleaningAndroidService.DONE_NOTIFY_CHANNEL_ID)
        .setContentTitle("卸载完成")
        .setSmallIcon(R.drawable.ic_outline_done_24).apply {
            if (list.size>0) Intent(context,ShowResult::class.java).let { intent ->
                setContentText("部分卸载失败")
                intent.putExtra(EXTRA_FAILED_LIST,list)
                PendingIntent.getActivity(context,0,intent,0)
            }.let { intent -> NotificationCompat.Action(R.drawable.ic_outline_done_24,"查看因果",intent) }
             .let { action -> addAction(action) }
        }.build()
        .let { notification -> notificationManager.notify(CleaningAndroidService.DONE_NOTIFY_ID,notification) }
    /**
     * 通知开始
     *
     */
    fun start() {
        val n = NotificationCompat.Builder(context, CleaningAndroidService.DONE_NOTIFY_CHANNEL_ID)
                .setContentTitle("卸载开始")
                .setSmallIcon(R.drawable.ic_outline_delete_forever_24)
                .build()
        notificationManager.notify(CleaningAndroidService.DONE_NOTIFY_ID,n)
    }
}