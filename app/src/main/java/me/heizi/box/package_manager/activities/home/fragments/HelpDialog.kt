package me.heizi.box.package_manager.activities.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ItemHelpBinding

class HelpDialog : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_help, container, false)
        if (view is RecyclerView) {
            with(view) {
                adapter = HelpRecyclerViewAdapter()
            }
        }
        return view
    }
    class HelpRecyclerViewAdapter : RecyclerView.Adapter<HelpRecyclerViewAdapter.ViewHolder>() {

        private val contents = listOf (
            Help(
                icon = R.drawable.ic_outline_help_outline_24,
                title = "帮助",
                context =
                """|    欢迎使用本软件，在本页面可以找到所有的使用教程。
            """.trimMargin()
            ), Help(
                icon = R.drawable.ic_outline_cleaning_services_24,
                title = "一键删除",
                context =
                """|    本软件提供快捷删除系统应用功能，但不提供卸载方案，卸载方案可以从网络获取，可以自己创建，一般来说本应用的酷安评论区会可以找到丰富的方案。如果你的屏幕够大可以看到有个图标在工具栏上方，或者点击菜单键点击导入，即可打开导入界面。在输入方案后即可批量卸载系统应用。
            """.trimMargin()
            ), Help(
                icon = R.drawable.ic_outline_save_24,
                title = "导出方案",
                context =
                """|    该功能能让你把你自己的优化方案分享给你的朋友，甚至可以做成Magisk模块（咕咕咕），你只需要点开在工具栏上方的菜单的导出按钮即可创建一个卸载方案版本，在创建完成后点击复制按钮即可复制得到一串经过处理的字符。请保证字符的完整，不然数据丢失时会让你的朋友无法使用你的卸载方案。
            """.trimMargin()
            ), Help(
                icon = R.drawable.ic_outline_delete_forever_24,
                title = "卸载",
                context = "这个相信不用我多说了，懂得都懂。"
            )
        )

        data class Help (
            val title:String,
            val context:String,
            @DrawableRes val icon:Int
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ItemHelpBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val model = contents[position]
            holder.binding.run {
                iconHelp.setImageDrawable(ContextCompat.getDrawable(root.context,model.icon))
                titleHelp.text = model.title
                contentHelp.text = model.context
            }
        }

        override fun getItemCount(): Int = contents.size

        inner class ViewHolder(val binding: ItemHelpBinding) : RecyclerView.ViewHolder(binding.root)

    }

}