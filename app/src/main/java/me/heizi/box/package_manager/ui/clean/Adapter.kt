package me.heizi.box.package_manager.ui.clean

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.bindText
import java.util.*

class Adapter:ListAdapter<UninstallInfo,Adapter.ViewHolder>(differ){



    class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var title by bindText(R.id.title_uninstall_info)
        var message by bindText(R.id.message_uninstall_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i(TAG, "onCreateViewHolder: called")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uninstall_info_input,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i(TAG, "onBindViewHolder: called $position")
        with(getItem(position)){ with(holder) {
            title =
            """ ${applicationName.takeIf { it!=packageName }?.plus(":")?:""} $applicationName """
            message =
            """|$sourceDirectory
               |${dataDirectory?:"没有数据路径可删除"}
                """.trimMargin()
            holder.itemView.findViewById<FrameLayout>(R.id.delete_uninstall_info_btn).setOnClickListener {
                removeItem(position)
            }
        } }
    }

    private var finalList:ArrayList<UninstallInfo> = ArrayList()

    override fun submitList(list: MutableList<UninstallInfo>?) {
        val arrayList = ArrayList<UninstallInfo>()
        list?.let { arrayList.addAll(it) }
        finalList = arrayList
        super.submitList(list)
    }
    private fun removeItem(position: Int) {
        finalList.removeAt(position)
        notifyItemRemoved(position)
        submitList(finalList)
        notifyItemRangeChanged(position-1,3)
    }
    companion object {
        private val differ = object : DiffUtil.ItemCallback<UninstallInfo>(){
            override fun areItemsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                oldItem.packageName == newItem.packageName
            override fun areContentsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                oldItem.equals(newItem)
        }
    }
}