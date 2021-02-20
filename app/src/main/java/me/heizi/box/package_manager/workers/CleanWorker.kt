package me.heizi.box.package_manager.workers
// FIXME: 2021/2/20 无法实现
//import android.annotation.SuppressLint
//import android.app.Notification
//import android.app.NotificationManager
//import android.content.Context
//import android.graphics.drawable.Icon
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import android.widget.TextView
//import androidx.core.app.NotificationCompat
//import androidx.core.view.isVisible
//import androidx.core.view.size
//import androidx.lifecycle.LifecycleService
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.google.android.material.bottomsheet.BottomSheetDialog
//import kotlinx.coroutines.Dispatchers.Default
//import kotlinx.coroutines.Dispatchers.IO
//import kotlinx.coroutines.Dispatchers.Main
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import me.heizi.box.package_manager.Application
//import me.heizi.box.package_manager.R
//import me.heizi.box.package_manager.activities.home.fragments.CleanDialog
//import me.heizi.box.package_manager.models.BackupType
//import me.heizi.box.package_manager.models.UninstallInfo
//import me.heizi.box.package_manager.repositories.CleaningAndroidService
//import me.heizi.box.package_manager.utils.uninstallAll
//import me.heizi.kotlinx.shell.CommandResult
//import me.heizi.kotlinx.shell.ProcessingResults
//
//class CleanWorker (
//        appContext: Context, params: WorkerParameters,
//        private val uninstallInfo: List<UninstallInfo>,
//        private val backup: BackupType,
//        private val mountString: String,
//) : CoroutineWorker(appContext, params) {
//
//    override suspend fun doWork(): Result = withContext(IO)r@{
//        launch(Main) { notifyStart() }
//        val sb = StringBuilder()
//        launch(Default) {
//
//        }.invokeOnCompletion {
//
//        }
//        uninstallAll(
//            uninstallInfo,backup,mountString,
//        ).collect { r->
//            when (r) {
//                is ProcessingResults.CODE -> TODO()
//                is ProcessingResults.Error -> TODO()
//                is ProcessingResults.Message -> TODO()
//                is ProcessingResults.Closed -> {
//                    cancel()
//                }
//            }
//        }
//        delay()
//
//        val n = NotificationCompat.Builder(applicationContext, CleaningAndroidService.DONE_NOTIFY_CHANNEL_ID)
//                .setContentTitle("卸载完成")
//                .setContentText("点击查看结果")
//                .setSmallIcon(R.drawable.ic_outline_done_24)
//                .build()
//        launch {  }
//
//        notificationManager.notify(CleaningAndroidService.DONE_NOTIFY_ID,n)
//        TODO("Not yet implemented")
//    }
//    private val notificationManager get() =  applicationContext.getSystemService(LifecycleService.NOTIFICATION_SERVICE) as NotificationManager
//    private val icon by lazy { Icon.createWithResource(applicationContext, R.drawable.ic_outline_notification_72) }
//
//    private val failed = ArrayList<CommandResult.Failed>()
//    /**
//     * 通知失败
//     *
//     * @param reason 原因是啥
//     * @param appName 应用名称
//     */
//    private fun notifyFailed(reason:String?, appName:String)  = Notification
//            .Builder(applicationContext, CleaningAndroidService.FAILED_NOTIFY_CHANNEL_ID)
//            .setLargeIcon(icon)
//            .setBadgeIconType(Notification.BADGE_ICON_SMALL)
//            .setSmallIcon(icon)
//            .setContentText(reason)
//            .setStyle(reason?.let {
//                Notification.BigTextStyle().setSummaryText(appName).bigText(reason).setBigContentTitle("卸载失败,原因:")
//            }?: Notification.BigTextStyle().setSummaryText(appName).bigText("卸载失败但无错误原因"))
//            .build()
//            .let {
//                notificationManager.notify(CleaningAndroidService.FAILED_TAG, failed.size,it)
//            }
//
//
//    /**
//     * 通知成功
//     *
//     * @param appName
//     */
//    private fun notifySuccess(appName:String) = Notification
//            .Builder(applicationContext, CleaningAndroidService.PROCESS_NOTIFY_CHANNEL_ID)
//            .setLargeIcon(icon)
//            .setSmallIcon(icon)
//            .setContentTitle("卸载成功....")
//            .setContentText(appName)
//            .build()
//            .let {
//                Log.i(Application.TAG, "notifySuccess: called")
//                notificationManager.notify(CleaningAndroidService.PROCESS_NOTIFY_ID,it)
//            }
//
//
//    /**
//     * 通知开始
//     *
//     */
//    private fun notifyStart() = NotificationCompat
//            .Builder(applicationContext, CleaningAndroidService.DONE_NOTIFY_CHANNEL_ID)
//            .setContentTitle("卸载开始")
//            .setSmallIcon(R.drawable.ic_outline_delete_forever_24)
//            .build().let {
//                notificationManager.notify(CleaningAndroidService.DONE_NOTIFY_ID,it)
//            }
//
//
//    private fun showAllFailedResultAsBottomSheet(context: Context,result: List<Pair<UninstallInfo, CommandResult.Failed>>, allresult:Int) {
//        val list = RecyclerView(context)
//        list.apply {
//            layoutManager = LinearLayoutManager(this.context)
//            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//                override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1
//                @SuppressLint("SetTextI18n")
//                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
//                        if (viewType == 0) FrameLayout(context).apply{
//                            addView(TextView(context).apply {
//                                setTextAppearance(android.R.style.TextAppearance_Large)
//                                text = "列表内有${allresult}个应用，一共成功卸载${list.size-allresult}个应用，卸载失败和原因："
//                            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
//                                gravity = Gravity.CENTER
//                            })
//                        }.let{object : RecyclerView.ViewHolder(it){} }
//                        else CleanDialog.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_uninstall_info_input,parent,false))
//                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//                    if (position!=0 && holder is CleanDialog.ViewHolder) with(result[position-1]) {
//                        holder.itemView.findViewById<View>(R.id.delete_uninstall_info_btn).isVisible = false
//                        holder.title = first.applicationName
//                        holder.message = StringBuilder("路径:").apply {
//                            append(first.sourceDirectory)
//                            append(";error:")
//                            append(second.code)
//                            second.processingMessage.takeIf { it.isNotEmpty() }?.let {
//                                append("\n")
//                                append("过程：")
//                                append(it)
//                            }
//                            second.errorMessage.takeUnless { it.isNullOrEmpty() }?.let {
//                                append("\n")
//                                append("错误：")
//                                append(it)
//                            }
//                        }.toString()
//                    }
//                }
//                override fun getItemCount(): Int =result.size+1
//            }
//        }
//        BottomSheetDialog(context).let {
//            it.setContentView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
//            it.show()
//        }
//    }
//}