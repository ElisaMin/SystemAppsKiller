package me.heizi.box.package_manager.activities.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.bindText
import java.util.*

/**
 * Adapter
 *
 * 用于[R.layout.dialog_clean]的recycler view 的Adapter
 * 每个item主要展示应用名和删除按钮
 */
open class EditUninstallListAdapter : ListAdapter<UninstallInfo, EditUninstallListAdapter.ViewHolder>(
        object : DiffUtil.ItemCallback<UninstallInfo>(){
            override fun areItemsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                    oldItem.packageName == newItem.packageName
            override fun areContentsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                    oldItem.equals(newItem) }
){

    private var finalList: ArrayList<UninstallInfo> = ArrayList()

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uninstall_info_input,parent,false)
        return ViewHolder(view)
    }

    final override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
    open fun onRemoveBtnClicked(position: Int) {
        finalList.remove(currentList[position])
        submitList(finalList)
    }

    /**
     * Submit list
     *
     * 把[list]扔到[finalList]
     */
    final override fun submitList(list: MutableList<UninstallInfo>?) {
        val arrayList = ArrayList<UninstallInfo>()
        list?.let { arrayList.addAll(it) }
        finalList = arrayList
        super.submitList(list)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var title by bindText(R.id.title_uninstall_info)
        var message by bindText(R.id.message_uninstall_info)
    }

}