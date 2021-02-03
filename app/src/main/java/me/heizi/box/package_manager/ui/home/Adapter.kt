package me.heizi.box.package_manager.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ItemAppUninstallBinding
import me.heizi.box.package_manager.models.DisplayingData
import me.heizi.box.package_manager.repositories.PackageRepository
import java.util.*

/**
 * Adapter
 *
 * 列表展示的适配器
 */

private class Adapter(private val uninstall:(DisplayingData.App,Int)->Unit,private val repository: PackageRepository): ListAdapter<DisplayingData, RecyclerView.ViewHolder>(differ),Filterable {




    private val filter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val result = FilterResults()
                val time = System.currentTimeMillis()
//                val list =
//                        if (constraint.isNullOrEmpty()) repository.displayingFlow.value
//                        else {
//                            val s = constraint.toString()
//                            adapter.currentList.filter {
//                                it is DisplayingData.App && diff(s,it.sDir,it.name)
//                            }
//                        }
//                result.count = list.size
//                result.values = list
                Log.i(Application.TAG, "performFiltering: ${System.currentTimeMillis() - time}")
                return result
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Log.i(Application.TAG, "publishResults: called ")
//                adapter.submitList(results?.values as List<DisplayingData>)
            }

        }
    }


    
    /**
     * [getItemViewType]根据[position] [getItem]判断返回[TITLE_VIEW]或者[APP_VIEW]
     */
    override fun getItemViewType(position: Int): Int {
        Log.i(TAG, "getItemViewType: $position")
        return if (getItem(position) is DisplayingData.Header) TITLE_VIEW else APP_VIEW
    }

    /**
     * 根据[viewType] 决定要返回[AppViewHolder]和[TitleViewHolder]之前的其中一个
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        
        return if (viewType == TITLE_VIEW) TitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_title_textview_only, parent, false) as FrameLayout)
        else AppViewHolder(ItemAppUninstallBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    private fun TitleViewHolder.bind(title:String) {
        layout.findViewById<TextView>(R.id.tv_item_title_only).text = title
    }

    private fun AppViewHolder.bind(data: DisplayingData.App, position: Int) {
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
    override fun getFilter(): Filter = filter

    companion object {

        const val TITLE_VIEW = 0x0001
        const val APP_VIEW = 0x0002

        /**
         * 模糊搜索
         *
         * todo 降低功耗
         * @param key
         * @param path
         * @param name
         */
        fun match(key:String,path: String,name: String):Boolean {
            if (path == key||name==key) return true
            val keys = key.trim().toLowerCase(Locale.CHINA).split(" ","\n","/")
            val names = name.trim().toLowerCase(Locale.CHINA).split(" ")
            for (n in names) for (k in keys) if (n.contains(k) ) return true
            val paths = path.trim().toLowerCase(Locale.CHINA).split(".","/","_","-")
            for (p in paths) for (k in keys) if (p.contains(k)) return true
            return false
        }
        
        val differ = object: DiffUtil.ItemCallback<DisplayingData>() {
            override fun areItemsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean {
                Log.i(Application.TAG, "areItemsTheSame: called ")
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
                Log.i(Application.TAG, "areContentsTheSame: called")
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

}

