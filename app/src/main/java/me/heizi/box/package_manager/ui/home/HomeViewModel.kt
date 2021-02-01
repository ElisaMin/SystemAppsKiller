package me.heizi.box.package_manager.ui.home

import android.app.Application
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ItemAppUninstallBinding
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.repositories.AppsPagingSource
import me.heizi.box.package_manager.repositories.PackageRepository
import java.util.*


class HomeViewModel(application: Application) : AndroidViewModel(application),Filterable {

    private lateinit var repository: PackageRepository
    private val pager by lazy {
        Pager(config = PagingConfig(20), pagingSourceFactory = {
            AppsPagingSource(application.packageManager,repository.systemAppsFlow.value)
        })
    }

    val adapter by lazy { Adapter() }


    /**
     * 正式启动
     *
     * @param packageRepository
     */
    fun start(packageRepository: PackageRepository) {
        repository = packageRepository
        viewModelScope.launch(Default) {
            pager.flow.collectLatest(adapter::submitData)
        }

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
                    val pagingData = if (constraint.isNullOrEmpty()) data
                    else {
                        fun format(string: CharSequence) = string.toString().trim().encodeToByteArray().toString(charset = Charsets.UTF_8).toLowerCase(Locale.ROOT)
                        val s = format(constraint)
                        data.filter {

                            it is DisplayingData.DisplayingApp && (format(it.name).contains(s) || format(it.sDir).contains(s))
                        }
                    }
                    Log.i(TAG, "publishResults: $constraint")
                    adapter.submitData(pagingData)
                    Log.i(TAG, "publishResults: updated")
                    cancel()
                }
//                pager.flow.singleOrNull()?.let { data -> //如果是空的咱直接emit
//                }?: application.longToast("搜索失败")
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
                    is DisplayingData.DisplayingApp -> {
                        if (newItem is DisplayingData.DisplayingApp) {
                            newItem.position == oldItem.position
                        }else false
                    }
                }
            }
            override fun areContentsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean {
                Log.i(TAG, "areContentsTheSame: called")
                return if (oldItem is DisplayingData.Header || newItem is DisplayingData.Header) true
                else oldItem as DisplayingData.DisplayingApp == newItem as DisplayingData.DisplayingApp
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
    inner class Adapter (

    ) : PagingDataAdapter<DisplayingData, RecyclerView.ViewHolder>(differ) {

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
        private fun AppViewHolder.bind(displayingData: DisplayingData.DisplayingApp) {
            binding.data = displayingData
            binding.uninstallBtn.setOnClickListener {
                TODO("等待本view model的卸载功能完善")
            }
        }



        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is TitleViewHolder -> holder.bind((getItem(position)as DisplayingData.Header).path)
                is AppViewHolder -> {
                    val a = getItem(position) as DisplayingData.DisplayingApp
                    holder.bind(a)
//                check(a.position)
                }
                else -> throw TypeCastException("viewHolder at $position is not AppsViewHolder or TitleViewHolder !!")
            }
        }

    }

    override fun getFilter(): Filter = filter


}