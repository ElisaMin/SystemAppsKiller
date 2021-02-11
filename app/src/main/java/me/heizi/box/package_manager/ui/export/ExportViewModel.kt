package me.heizi.box.package_manager.ui.export

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest
import me.heizi.box.package_manager.Application.Companion.PACKAGE_NAME
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.dao.DB
import me.heizi.box.package_manager.dao.DB.Companion.databaseMapper
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.Version
import me.heizi.box.package_manager.models.VersionConnected
import me.heizi.box.package_manager.utils.*
import java.util.*

class ExportViewModel(a: Application) : AndroidViewModel(a) {

//    interface Repository {
//
//    }
//    lateinit var repository: Repository


    val adapter by lazy {
        Adapter(viewModelScope,differ = object :DiffUtil.ItemCallback<Version>() {
            override fun areItemsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem == newItem
        })
    }

    init {
        viewModelScope.launch(IO) {
            databaseMapper {
                getALlVersions()
            }.await().let { flow ->
                flow.collectLatest { list -> launch(Main) { adapter.submitList(list) } }
            }
        }
    }


    fun createNewVersion(context: Context) = viewModelScope.launch(IO) {
        Log.i(TAG, "createNewVersion: called")
        val count = databaseMapper {
            getUninstalledCount()
        }
        val createDialog = async(Main) {
            val editText = EditText(context)
            context.dialog(
                DialogBtns.Positive("添加") { _, _ ->
                    Log.i(TAG, "createNewVersion: click is will")
                    val name = editText.text.toString()
                    val time = System.currentTimeMillis().toInt()
                    val recordVersion = Version(name = name, createTime = time)
                    MainScope().run {
                        launch(IO) {
                            Log.i(TAG, "createNewVersion: started recording")
                            DB.INSTANCE.getDefaultMapper().add(recordVersion)
                        }.invokeOnCompletion { launch(IO) { with(DB.INSTANCE.getDefaultMapper()) {
                            Log.i(TAG, "createNewVersion: completion")
                            val version = findVersion(recordVersion.name, recordVersion.createTime)?.id ?: 0
                            Log.i(TAG, "createNewVersion: collect before")
                            getAllUninstalled().collectLatest {
                                Log.i(TAG, "createNewVersion: on collection")
                                var i = 0

                                it.forEach {
                                    add(Connect(version = version, record = it.id!!))
                                    i++
                                }
                                launch(Main) {
                                    app.shortToast("添加成功,本次添加了${i}个应用，费时${System.currentTimeMillis() - time}。")
                                    this.cancel()
                                }
                            }
                        } } }
                    }
                },
                show = false,
                view = editText,
                title = "请输入名称",
            )
        }
        val counts = count.await()
        launch(Main) {
            if (counts<=0) app.shortToast("没有屑载过任何应用哦~")
            else createDialog.await().show()
        }

    }



    class Adapter(private val scope: CoroutineScope,differ:DiffUtil.ItemCallback<Version>):ListAdapter<Version,Adapter.ViewHolder>(differ) {
        private val timeFormat by lazy { SimpleDateFormat("MM/dd/hh:mm ", Locale.CHINA) }
        class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView) {
            var title by bindText(R.id.title_content_view)
            var time by bindText(R.id.text_content_view)
            var copyAction by bindClick(R.id.btn_copy)
            var editAction by bindClick(R.id.btn_edit)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder) {
                with(getItem(position)) {
                    time = timeFormat.format(Date(createTime.toLong()))
                    title = name
                    copyAction = {
                        copy(this, itemView.context)
                    }
                    editAction = {
                        edit(this, itemView.context)
                    }
                }
            }
        }

        /**
         * Edit
         *
         * @param version
         */
        // FIXME: 2021/2/10 找到一个方法修改该版本
        private fun edit(version: Version,context: Context) {
            context.shortToast("未实现")
        }

        private fun startProgressing() {
            //未实现
        }
        private fun stopProgressing() {
            //未实现
        }

        /**
         * Copy
         *
         * 弹出Dialog给内容一个复制的机会 或者直接bang的一下直接复制
         * @param version
         */
        private fun copy(version: Version,context: Context){
            startProgressing()
            scope.updateDB {
                val l = getDefaultMapper().findVersionUninstallList(version.id!!)
                try {
                    val v = VersionConnected(version.id, version.name, false, version.createTime, l)
                    val s = scope.async{ Compressor.generateV1(v) }
                    scope.launch(Main) {
                        (context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("$PACKAGE_NAME.copyText",s.await()))
                        context.shortToast("复制成功")
                        stopProgressing()
                    }
                }catch (e:Exception) {
                    Log.i(TAG, "copy: failed",e)
                    scope.launch(Main) {
                        context.longToast("失败：${e.message}")
                    }

                    stopProgressing()
                }
            }
            scope.launch(Default) {

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_title_content_view,parent,false))
    }
}