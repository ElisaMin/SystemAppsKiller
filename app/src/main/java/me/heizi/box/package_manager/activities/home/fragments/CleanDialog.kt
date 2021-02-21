package me.heizi.box.package_manager.activities.home.fragments

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.DEFAULT_MOUNT_STRING
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.home.HomeActivity.Companion.parent
import me.heizi.box.package_manager.databinding.DialogCleanBinding
import me.heizi.box.package_manager.models.BackupType
import me.heizi.box.package_manager.models.JsonContent
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.box.package_manager.utils.*
import java.util.*

class CleanDialog : BottomSheetDialogFragment() {
    private val binding by lazy { DialogCleanBinding.bind(requireView()) }
     private val differ = object : DiffUtil.ItemCallback<UninstallInfo>(){
        override fun areItemsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
            oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
            oldItem.equals(newItem)
    }

    val viewModel = ViewModel()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? { return layoutInflater.inflate(R.layout.dialog_clean,container) }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        viewModel.startCollect()
        viewModel.stopProgress()
    }


    /**
     * On done btn clicked
     *
     * 当done的按钮点击后判断是否可卸载,并且调用dialog让用户选择type启动bind的程序然后冲
     */
    private fun onDoneBtnClicked() {
        if (viewModel.isUninstallable.value) callDialogGetBackupType {
            val list = viewModel.adapter.currentList
            val mountString = parent.viewModel.preferences.mountString ?: DEFAULT_MOUNT_STRING
            class C : ServiceConnection {
                val context:Context = parent
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder is CleaningAndroidService.Binder) lifecycleScope.launch(Main) {
                        //调用binder的函数
                        binder.startUninstall(list,it,mountString){
                            context.applicationContext.unbindService(this@C)
                        }
                        context.shortToast("正在开始卸载一共${list.size}个应用")
                        dismiss()
                    } else throw IllegalArgumentException("binder非来自本应用")
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            val connect = C()
            CleaningAndroidService.intent(requireContext().applicationContext).let {i->
                context?.applicationContext?.let {
                    it.startService(i)
                    it.bindService(i,connect, Context.BIND_AUTO_CREATE)
                }
            }
        }
    }

    private fun ViewModel.startCollect() = lifecycleScope.launch(Dispatchers.Default) {
        textInput.collectLatest {
            onInputting(it)
        }
    }

    /**
     * 在文本输入的时候超过六个触发机制检查是否可卸载 否则不可卸载(((
     *
     * @param input
     */
    private fun ViewModel.onInputting(input:String) {
        if (input.length <= 6) { showErrorMessage(null);return } else { startProgress() }
        try {
            val jsonContent = Compressor.buildJson(input)
            onUninstallInfoChanged(jsonContent)
            uninstallable()
            showErrorMessage(null)
        }catch (e:Exception) {
            Log.i(Application.TAG, "wrongWithDecoding ",e)
            inuninstallable()
            showErrorMessage(e.message)
            onUninstallInfoChanged(null)
        } finally {
            stopProgress()
        }
    }

    /**
     * 在uninstall info 改变时往adapter放东西
     *
     * @param json
     */
    private fun ViewModel.onUninstallInfoChanged(json:JsonContent?) = lifecycleScope.launch(Main) {
        adapter.submitList(json?.apps?.toMutableList())
        Log.i(Application.TAG, "list: changed on cleaning")
    }

    /**
     * Call dialog get backup type
     *
     * 弹出窗口让用户选择备份模式后执行[block]回调
     */
    private fun callDialogGetBackupType(block:(BackupType)->Unit) {
        requireContext().dialogBuilder(
                title = "请选择备份模式",
        ).apply {
            setItems(arrayOf("不备份","把apk改名为apk.bak","把APK移动到内部存储空间")) {_,i ->
                when(i) {
                    0 -> BackupType.JustRemove
                    1 -> BackupType.BackupWithOutPath
                    else -> BackupType.BackupWithPath.Default
                }.let(block)
            }
        }.show()
    }
    class ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        var title by bindText(R.id.title_uninstall_info)
        var message by bindText(R.id.message_uninstall_info)
    }
    /**
     * Adapter
     *
     * 用于[R.layout.dialog_clean]的recycler view 的Adapter
     * 每个item主要展示应用名和删除按钮
     */
    inner class Adapter: ListAdapter<UninstallInfo, ViewHolder>(differ){

        private var finalList: ArrayList<UninstallInfo> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uninstall_info_input,parent,false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(getItem(position)){ with(holder) {
                title = applicationName
                message = "$packageName\n$sourceDirectory"
                holder.itemView.findViewById<FrameLayout>(R.id.delete_uninstall_info_btn).setOnClickListener { onRemoveBtnClicked(position) }
            } }
        }

        /**
         * On remove btn clicked
         *
         * 当index为[position]的item被通知删除时把[finalList]的删除掉,并更新[finalList]
         */
        private fun onRemoveBtnClicked(position: Int) {
            finalList.remove(currentList[position])
            submitList(finalList)
        }

        /**
         * Submit list
         *
         * 把[list]扔到[finalList]
         */
        override fun submitList(list: MutableList<UninstallInfo>?) {
            val arrayList = ArrayList<UninstallInfo>()
            list?.let { arrayList.addAll(it) }
            finalList = arrayList
            super.submitList(list)
        }

    }

    inner class ViewModel{
        val adapter: Adapter by lazy { Adapter() }
        val textInput: MutableStateFlow<String> = MutableStateFlow("")
        val helpText get() = _helpText.asStateFlow()
        val isUninstallable get() = _isUninstallable.asStateFlow()
        val progressing get() = _progressing.asStateFlow()
        private val _helpText = MutableStateFlow<String?>(null)
        private val _isUninstallable = MutableStateFlow(false)
        private val _progressing = MutableStateFlow(true)
        fun onDoneBtnClicked() { this@CleanDialog.onDoneBtnClicked() }
        fun startProgress() { _progressing set true }
        fun stopProgress() { _progressing set false }
        fun showErrorMessage(error: String?) { _helpText set error }
        fun uninstallable() { _isUninstallable set true }
        fun inuninstallable() { _isUninstallable set false }
    }
}
