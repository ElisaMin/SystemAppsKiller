package me.heizi.box.package_manager.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ItemAppUninstallBinding
import me.heizi.box.package_manager.models.DisplayingData

class Adapter (

) : PagingDataAdapter<DisplayingData, RecyclerView.ViewHolder>(differ) {

    companion object {
        const val TITLE_VIEW = 0x0001
        const val APP_VIEW = 0x0002
        val differ = object: DiffUtil.ItemCallback<DisplayingData>() {
            override fun areItemsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean = when(oldItem) {
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
            override fun areContentsTheSame(oldItem: DisplayingData, newItem: DisplayingData): Boolean
            = if (oldItem is DisplayingData.Header || newItem is DisplayingData.Header) true
             else oldItem as DisplayingData.DisplayingApp == newItem as DisplayingData.DisplayingApp
        }
    }

    /**
     * Binding的卸载之类的东西
     */
    class AppViewHolder(val binding: ItemAppUninstallBinding): RecyclerView.ViewHolder(binding.root)
    /**
     * 展示标题用的ViewHolder
     */
    class TitleViewHolder(val layout:FrameLayout): RecyclerView.ViewHolder(layout)

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
    }

//    val hashMap = HashMap<Int,String?>()
//    fun check(int: Int) {
//        if(hashMap[int]!=null) {
//            Log.i(TAG, "check: 发现重复$int")
//        }
//        hashMap[int] = "not null$int"
//
//    }


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