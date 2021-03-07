package me.heizi.box.package_manager.activities.home.fragments

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collectLatest
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.home.HomeActivity.Companion.parent
import me.heizi.box.package_manager.custom_view.BottomSheetDialogFragment
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version
import me.heizi.box.package_manager.databinding.DialogExportBinding
import me.heizi.box.package_manager.models.VersionConnected
import me.heizi.box.package_manager.utils.*
import me.heizi.box.package_manager.utils.Compressor.toQrCode
import me.heizi.box.package_manager.utils.Compressor.toShareableText
import me.heizi.kotlinx.android.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.filter
import kotlin.collections.find
import me.heizi.box.package_manager.dao.DB as db

class ExportDialog : BottomSheetDialogFragment<DialogExportBinding>() {
    override val binding by lazy { DialogExportBinding.inflate(layoutInflater) }
    private val adapter = Adapter()
    private val differ get() = object :DiffUtil.ItemCallback<Version>() {
        override fun areItemsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem == newItem
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        default {
            db.asVersions.collectLatest { list -> launch(Dispatchers.Main) { adapter.submitList(list) } }
        }
        binding.listUninstallVersionsExport.adapter = adapter
        binding.addNewVersionBtnExport.setOnClickListener { onCreateBtnCLicked() }
    }

    private fun onCreateBtnCLicked () {
        Log.i(TAG, "onNewVersionCalled: called")
        val count = db.versions.size
        Log.i(TAG, "onNewVersionCalled: result $count ")
        if (count <= 0) requireContext().shortToast("没有屑载过任何应用哦~")
        else showDialogCreateVersion()
    }

    private fun showDialogCreateVersion() {
        val packageName = HashMap<String,UninstallRecord>()
        for (uninstallRecord in db.uninstalleds) packageName[uninstallRecord.packageName] = uninstallRecord
        VersionEditDialog(
            defaultName = "新版本",
            sourceList = ArrayList(packageName.values) ,
            beforeItemRemove = {true}
        ) { list,name->
            try {
                val source = db.uninstalleds
                val ids: List<Int> = list.map { i -> source.find { it.packageName == i.packageName } }.filterNotNull().map { it.id!! }
                if (ids.isEmpty()) throw NullPointerException("列表为空")
                createNewVersion(name,ids)
                true
            } catch (e: Exception) {
                Log.i(TAG, "edit: ",e)
                false
            }
        }.show(parent.supportFragmentManager,"create")
    }

    private fun notifySameName() {

    }
    private fun createNewVersion(name:String,uninstallInfoIDs:List<Int>) = io {
        for (i in db.versions) if (name == i.name) {
            notifySameName()
            return@io
        }
        val time = System.currentTimeMillis().toInt()
        db + Version(name = name, createTime = time)

        db.asVersions.collectLatest {
            for (i in it) if (i.name == name) {
                val version = i.id!!
                uninstallInfoIDs.forEach { uninstalled ->
                    db + Connect(version = version,record = uninstalled )
                }
                launch(Dispatchers.Main) { context?.longToast("添加成功,本次添加了${uninstallInfoIDs.size}个应用，费时${(System.currentTimeMillis() - time)/1000}秒。") }
            }
        }
    }

    companion object {
        private const val EDIT = "编辑"
        private const val COPY = "复制文本"
    }
    private inner class Adapter: ListAdapter<Version, Adapter.ViewHolder>(differ) {
        private val timeFormat by lazy { SimpleDateFormat("MM/dd/hh:mm ", Locale.CHINA) }
        inner class ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView) {
            var title by bindText(R.id.title_content_view)
            var time by bindText(R.id.text_content_view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) { with(holder) { with(getItem(position)) {
            time = timeFormat.format(Date(createTime.toLong()))
            title = name
            holder.itemView.setOnClickListener {
                startProgressing()
                val version = this
                connected {
                    val count = it.apps.size
                    var help:String? = null
                    val bitmap = it
                        .runCatching { toQrCode() }
                        .onFailure { e -> help = "${e.javaClass.name}:${e.message}" }
                        .onSuccess { bitmap ->
                            if (bitmap == null) help = "数据太大 无法转换成二维码，期待往下的更新。"
                        }.getOrNull()
                    main {
                        ShowVersionInfoDialog(
                            ShowVersionInfoDialog.ViewModel(
                                EDIT to {edit(version)},
                                COPY to {copy(version)},
                                bitmap = bitmap,
                                version = version,
                                listSize = count,
                                helpText = help
                            )
                        ).show(parent.supportFragmentManager,"show_version_info")
                        stopProgressing()
                    }
                }
            }
        } } }

        /**
         * Edit
         *
         * @param version
         */
        private fun edit(version: Version) = version.connected { v->
            main {
                VersionEditDialog(
                        ArrayList(v.apps),
                        defaultName = v.name,
                        beforeItemRemove = { info ->
                            try {
                                withContext(IO) {
                                    db.uninstalleds.find { it.packageName == info.packageName }.let { r -> db.connections.find { it.version == v.id && it.record == r!!.id!! }?.let {  db - it } }
                                    true
                                }
                            } catch (e: NullPointerException) {
                                Log.i(TAG, "edit: null ", e)
                                false
                            } catch (e: Exception) {
                                Log.i(TAG, "edit: ",e)
                                false
                            }
                        }
                ) b@{ _,name ->
                    try {
                        if (name.isNotEmpty()&&name!=version.name)  //如果不是空的和不一样就:检查是否有重名
                            if(db.versions.any { it.id != version.id && it.name == version.name }) {
                                main { context?.shortToast("名字不可重复") }
                                false
                            } else {
                                io { db.UPDATE(version.copy(name = name)) }
                                main { context?.shortToast("名字更新成功") }
                                true
                            }
                        else {
                            main {
                                context?.longToast("名字为空")
                            }
                            false
                        }
                    } catch (e: Exception) {
                        Log.i(TAG, "edit: ",e)
                        false
                    }
                }.show(parent.supportFragmentManager,"edit")
                dismiss()
            }
        }

        private val awaitDialog by lazy { context!!.dialog(view = ProgressBar(context),cancelable = false,show = false) }
        private fun startProgressing() = main {
            awaitDialog.show()
        }
        private fun stopProgressing() = main {
            awaitDialog.dismiss()
        }

        /**
         * Copy
         *
         * 弹出Dialog给内容一个复制的机会 或者直接bang的一下直接复制
         * @param version
         */
        private fun copy(version: Version) = version.connected { v-> default { v.toShareableText().let(::share) } }

        private fun notifyEmptyList() {

        }

        fun Version.connected(block:suspend CoroutineScope.(VersionConnected)->Unit) = io {
            val versionId = this@connected.id
            fun <T> checkEmpty(list: List<T>) {if (list.isEmpty()) throw NullPointerException("该版本不存在数据库!!!")}
            startProgressing()
            try {
                db.connections.filter { it.version == versionId }.let { list ->
                    checkEmpty(list)
                    //已经拿到过滤后的Connect 根据connect里面的id找uninstall info 现在有两个List 找交集
                    // FIXME: 2021/2/23 提升性能
                    val ids = list.map { it.record }
                    val l = db.uninstalleds.filter { ids.contains(it.id) }
                    block(VersionConnected(list.first().version, name, false, createTime, l))
                }
            }catch (e:Exception) {
                Log.i(TAG, "connect: failed",e)
                launch(Dispatchers.Main) {
                    context?.longToast("失败：${e.message}")
                }
            }finally {
                stopProgressing()
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_title_content_view,parent,false))
    }


    /**
     * 分享文字
     *
     * @param text
     */
    @SuppressLint("UseRequireInsteadOfGet")
    private fun share(text:String) = main {
        (context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("${Application.PACKAGE_NAME}.copyText",text))
        context!!.shortToast("复制成功")
    }
}