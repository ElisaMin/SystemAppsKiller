package me.heizi.box.package_manager.ui.clean

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.bindText

class Adapter:ListAdapter<UninstallInfo,Adapter.ViewHolder>(differ){



    class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        var title by bindText(R.id.title_uninstall_info)
        var message by bindText(R.id.message_uninstall_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uninstall_info_input,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(getItem(position)){ with(holder) {
            title =
            """ ${applicationName.takeIf { it!=packageName }?.plus(":")?:""} $applicationName """
            message =
            """|$sourceDirectory
               |${dataDirectory?:"没有数据路径可删除"}
                """.trimMargin()
        } }
    }
    companion object {
        private val differ = object : DiffUtil.ItemCallback<UninstallInfo>(){
            override fun areItemsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                oldItem.packageName == newItem.packageName
            override fun areContentsTheSame(oldItem: UninstallInfo, newItem: UninstallInfo): Boolean =
                oldItem==newItem
        }
    }
}