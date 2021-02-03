package me.heizi.box.package_manager.ui.home

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.filter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.Application.Companion.app
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.databinding.ItemAppUninstallBinding
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.models.PreferencesMapper
import me.heizi.box.package_manager.repositories.AppsPagingSource
import me.heizi.box.package_manager.repositories.AppsPagingSource.Companion.LOAD_SIZE
import me.heizi.box.package_manager.repositories.PackageRepository
import me.heizi.box.package_manager.utils.isUserApp
import me.heizi.box.package_manager.utils.uninstallByShell
import me.heizi.kotlinx.shell.CommandResult
import java.util.*


class HomeViewModel( application: Application) : AndroidViewModel(application),Filterable {

    private lateinit var repository: PackageRepository
    private lateinit var mapper: PreferencesMapper
    private lateinit var currentPagingSource:AppsPagingSource
    private val pager by lazy {
        Pager(
            PagingConfig(
                pageSize = LOAD_SIZE,
                prefetchDistance = 30
            )
        ) {
            currentPagingSource = AppsPagingSource(app.packageManager,repository.systemAppsFlow)
            currentPagingSource
        }
    }

    val adapter by lazy { Adapter() }

    /**
     * 正式启动
     */
    fun start(packageRepository: PackageRepository,mapper: PreferencesMapper) {
        repository = packageRepository
        this.mapper = mapper
        viewModelScope.launch(Default) {
            pager.flow.collectLatest(adapter::submitData)
        }
        viewModelScope.launch(Unconfined) {
            repository.systemAppsFlow.collectLatest {
                Log.i(TAG, "start: changed")
                adapter.refresh()
            }
        }
        viewModelScope.launch(Unconfined) {
            adapter.loadStateFlow.collectLatest {
                Log.i(TAG, "load state: $it")
            }
        }

    }


    sealed class Status {
        object Nothings:Status()
        class Uninstalling(val data: DisplayingData.App):Status()
        class Failed(val data:CommandResult.Failed):Status()
        class Success(val appName: String):Status()
    }
    val status by lazy { MutableSharedFlow<Status>() }

    /**
     * 是否需要备份
     * @return 空为不需要 如果需要Backup的话就放个前path 就是/sdcard/abc/{apk}
     */
    // TODO: 2021/2/3 完成
    private fun getBackupInfo():String?{
        TODO("等待完成")
    }

    /**
     * Get data path
     *
     * 判断data path是否需要删除
     * @param path
     * @return 空时不需要删除 有就删
     */
    // TODO: 2021/2/3 完成
    private fun getDataPath(path: String):String? {
        return null
    }


    private fun uninstall(applicationInfo: ApplicationInfo,appName: String) =viewModelScope.launch(IO){
        //判断是否为系统应用
        val isSystemApp = !applicationInfo.isUserApp
        //判断是否需要备份
        val backupString = getBackupInfo()
        //开始卸载
        if (isSystemApp) {
            val isBackup = backupString!=null
            val data = getDataPath(applicationInfo.dataDir)
            val result = uninstallByShell(
                    sourceDirectory = applicationInfo.sourceDir,
                    dataDirectory = data,
                    backupPath = backupString,
                    mountString = mapper.mountString!!
            )
            val record = UninstallRecord(
                    name = appName,
                    packageName = applicationInfo.packageName,
                    source =  applicationInfo.sourceDir,
                    data = data,
                    isBackups = isBackup
            )
            //卸载完成
            when(val r = result.await()) {
                is CommandResult.Success -> onUninstallSuccess(r,record)
                is CommandResult.Failed -> onUninstallFailed(r)
            }
        }else {
            // TODO: 2021/2/3 卸载普通应用
        }
    }

    /**
     * 当卸载成功时:
     *
     * @param result
     * @param record
     */
    private fun onUninstallSuccess(result: CommandResult.Success,record: UninstallRecord) {

    }

    /**
     * 当卸载失败时:
     *
     * 通知界面,展示[result]的错误.
     */
    private fun onUninstallFailed(result:CommandResult.Failed){

    }


    /**
     * 被界面通知到了调用的卸载
     *
     * @param data
     * @param position
     */
    private fun uninstall(data:DisplayingData.App,position: Int) {
        repository.systemAppsFlow.value.removeAt(data.position)
//        currentPagingSource.invalidate()
        adapter.notifyItemRemoved(position)
        Log.i(TAG, "uninstall: $position,$data")
        viewModelScope.launch(Default) {

        }
    }

    /**
     * 模糊搜索
     *
     * todo 降低功耗
     * @param key
     * @param path
     * @param name
     */
    fun diff(key:String,path: String,name: String):Boolean {
        if (path == key||name==key) return true
        val keys = key.trim().toLowerCase(Locale.CHINA).split(" ","\n","/")
        val names = name.trim().toLowerCase(Locale.CHINA).split(" ")
        for (n in names) for (k in keys) if (n.contains(k) ) return true
        val paths = path.trim().toLowerCase(Locale.CHINA).split(".","/","_","-")
        for (p in paths) for (k in keys) if (p.contains(k)) return true
        return false
    }

    private val filter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                Log.i(TAG, "performFiltering: result")
                return FilterResults()
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) { viewModelScope.launch(Default) {
                Log.i(TAG, "publishResults: called ")
                pager.flow.collectLatest { data ->
                    val time = System.currentTimeMillis()
                    val pagingData = if (constraint.isNullOrEmpty()) data
                    else {
                        val s = constraint.toString()
                        data.filter {
                            it is DisplayingData.App && diff(s,it.sDir,it.name)
                        }
                    }
                    Log.i(TAG, "publishResults: ${System.currentTimeMillis() - time}") 
                    adapter.submitData(pagingData)
                    cancel()
                }
            } }

        }
    }



    companion object {
        const val TITLE_VIEW = 0x0001
        const val APP_VIEW = 0x0002
        val differ = object: DiffUtil.ItemCallback<DisplayingData>() {
            override fun areItemsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean {
                Log.i(TAG, "areItemsTheSame: called ")
                return when(oldItem) {
                    is DisplayingData.Header -> {
                        if (newItem is DisplayingData.Header)  {
                            newItem.path == oldItem.path
                        } else false
                    }
                    is DisplayingData.App -> {
                        if (newItem is DisplayingData.App) {
                            newItem.position == oldItem.position
                        }else false
                    }
                }
            }
            override fun areContentsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean {
                Log.i(TAG, "areContentsTheSame: called")
                return if (oldItem is DisplayingData.Header || newItem is DisplayingData.Header) true
                else oldItem as DisplayingData.App == newItem as DisplayingData.App
            }
        }
    }

    /**
     * Binding的卸载之类的东西
     */
    class AppViewHolder(val binding: ItemAppUninstallBinding): RecyclerView.ViewHolder(binding.root)
    /**
     * 展示标题用的ViewHolder
     */
    class TitleViewHolder(val layout: FrameLayout): RecyclerView.ViewHolder(layout)


    /**
     * Adapter
     *
     * 列表展示的适配器
     */
    inner class Adapter : PagingDataAdapter<DisplayingData, RecyclerView.ViewHolder>(differ) {

        /**
         * [getItemViewType]根据[position] [getItem]判断返回[TITLE_VIEW]或者[APP_VIEW]
         */
        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) is DisplayingData.Header) TITLE_VIEW else APP_VIEW
        }

        /**
         * 根据[viewType] 决定要返回[AppViewHolder]和[TitleViewHolder]之前的其中一个
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                if (viewType == TITLE_VIEW) TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_title_textview_only,parent,false) as FrameLayout)
                else AppViewHolder(ItemAppUninstallBinding.inflate(LayoutInflater.from(parent.context),parent,false))

        private fun TitleViewHolder.bind(title:String) {
            layout.findViewById<TextView>(R.id.tv_item_title_only).text = title
        }




        /**
         * Bind
         *
         * 绑定时
         */
        private fun AppViewHolder.bind(data: DisplayingData.App,position: Int) {
            binding.data = data
            binding.uninstallBtn.setOnClickListener {
                uninstall(data,position)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is TitleViewHolder -> holder.bind((getItem(position)as DisplayingData.Header).path)
                is AppViewHolder -> {
                    val a = getItem(position) as DisplayingData.App
                    holder.bind(a,position)
                }
                else -> throw TypeCastException("viewHolder at $position is not AppsViewHolder or TitleViewHolder !!")
            }
        }

    }

    override fun getFilter(): Filter = filter


}