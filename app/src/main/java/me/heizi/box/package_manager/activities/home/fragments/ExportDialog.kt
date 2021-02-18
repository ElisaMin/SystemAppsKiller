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
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collectLatest
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.dao.DB
import me.heizi.box.package_manager.dao.DB.Companion.databaseMapper
import me.heizi.box.package_manager.dao.DB.Companion.updateDB
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.Version
import me.heizi.box.package_manager.databinding.DialogExportBinding
import me.heizi.box.package_manager.models.VersionConnected
import me.heizi.box.package_manager.utils.*
import java.util.*

class ExportDialog : BottomSheetDialogFragment() {
    private val binding by lazy { DialogExportBinding.inflate(layoutInflater) }
    private val adapter = Adapter()
    private val differ get() = object :DiffUtil.ItemCallback<Version>() {
        override fun areItemsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Version, newItem: Version): Boolean = oldItem == newItem
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return binding.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(IO) {
            databaseMapper {
                getALlVersions()
            }.await().let { flow ->
                flow.collectLatest { list -> launch(Dispatchers.Main) { adapter.submitList(list) } }
            }
        }
        binding.listUninstallVersionsExport.adapter = adapter
        binding.addNewVersionBtnExport.setOnClickListener(::onNewVersionCalled)
    }

    private fun onNewVersionCalled(view: View)  { lifecycleScope.launch(IO) {
        val count = withContext(IO) { DB.INSTANCE.getDefaultMapper().getUninstalledCount() }
        launch(Dispatchers.Main) {
            if (count <= 0) context?.shortToast("没有屑载过任何应用哦~")
            else {
                val editText = EditText(context)
                context?.dialog(
                    DialogBtns.Positive("添加") { _, _ -> createNewVersion(editText.text.toString()) },
                    show = false,
                    view = editText,
                    title = "请输入名称",
                )
    } } } }

    private fun createNewVersion(name:String) = lifecycleScope.launch(IO){
        val m = DB.INSTANCE.getDefaultMapper()
        val time = System.currentTimeMillis().toInt()
        m.add(Version(name = name, createTime = time))
        val id = m.findVersionID(name, time) ?: 0
        var i = 0
        m.getAllUninstalled().forEach {
            m.add(Connect(version = id, record = it.id!!))
            i++
        }
        launch(Dispatchers.Main) { context?.shortToast("添加成功,本次添加了${i}个应用，费时${System.currentTimeMillis() - time}。") }
    }

    private inner class Adapter: ListAdapter<Version, Adapter.ViewHolder>(differ) {
        private val timeFormat by lazy { SimpleDateFormat("MM/dd/hh:mm ", Locale.CHINA) }
        inner class ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView) {
            var title by bindText(R.id.title_content_view)
            var time by bindText(R.id.text_content_view)
            var copyAction by bindClick(R.id.btn_copy)
            var editAction by bindClick(R.id.btn_edit)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) { with(holder) { with(getItem(position)) {
            time = timeFormat.format(Date(createTime.toLong()))
            title = name
            copyAction = {
                copy(this, )
            }
            editAction = {
                edit()
            }
        } } }

        /**
         * Edit
         *
         * @param version
         */
        // FIXME: 2021/2/10 找到一个方法修改该版本
        private fun edit(/**version: Version**/) {
            context?.shortToast("未实现")
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
        private fun copy(version: Version,){
            startProgressing()
            lifecycleScope.updateDB {
                val l = getDefaultMapper().findVersionUninstallList(version.id!!)
                lifecycleScope.run {
                    try {
                        val v = VersionConnected(version.id, version.name, false, version.createTime, l)
                        val s = async{ Compressor.generateV1(v) }
                        //更新
                        launch(Dispatchers.Main) { copyTextToClipboard(s.await()) }
                    }catch (e:Exception) {
                        Log.i(Application.TAG, "copy: failed",e)
                        launch(Dispatchers.Main) {
                            context?.longToast("失败：${e.message}")
                        }
                    }finally {
                        stopProgressing()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_title_content_view,parent,false))
    }


    @SuppressLint("UseRequireInsteadOfGet")
    private fun copyTextToClipboard(text:String) {
        (context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("${Application.PACKAGE_NAME}.copyText",text))
        context!!.shortToast("复制成功")
    }
}