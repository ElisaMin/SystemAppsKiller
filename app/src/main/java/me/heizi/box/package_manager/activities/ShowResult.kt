package me.heizi.box.package_manager.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.heizi.box.package_manager.Application
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.activities.home.adapters.EditUninstallListAdapter
import me.heizi.box.package_manager.repositories.CleaningAndroidService
import me.heizi.kotlinx.android.dialog
import me.heizi.kotlinx.shell.CommandResult

class ShowResult : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(Application.TAG, "onReceive: show result broadcast work functionally")
        intent.getParcelableArrayListExtra<CommandResult.Failed>(CleaningAndroidService.EXTRA_FAILED_LIST)?.let {
            showAllFailedResultAsBottomSheet(it)
            Log.i(Application.TAG, "onReceive: not null and luanched")
        } ?: Log.i(Application.TAG, "onReceive: list didnt catched")

    }
    private fun showAllFailedResultAsBottomSheet(result: List<CommandResult.Failed>) {
        Log.i(Application.TAG, "showAllFailedResultAsBottomSheet: called")
        val list = RecyclerView(this)
        list.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1
                @SuppressLint("SetTextI18n")
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                    if (viewType == 0) FrameLayout(context).apply{
                        addView(TextView(context).apply {
                            setTextAppearance(android.R.style.TextAppearance_Large)
                            textAlignment = View.TEXT_ALIGNMENT_CENTER
                            text = "一共失败${list.size}个任务"
                            setPadding(16,16,16,16)
                        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.CENTER
                        })
                    }.let{object : RecyclerView.ViewHolder(it){} }
                    else EditUninstallListAdapter.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_uninstall_info_input, parent, false))
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val realPosition = position-1
                    if (position!=0 && holder is EditUninstallListAdapter.ViewHolder) with(result[position-1]) {
                        holder.itemView.findViewById<View>(R.id.delete_uninstall_info_btn).isVisible = false
                        holder.title = "错误#$realPosition: $code"
                        holder.message = StringBuilder().apply {
                            processingMessage.takeIf { it.isNotEmpty() }?.let {
                                append("过程：")
                                append(it)
                            }
                            errorMessage.takeUnless { it.isNullOrEmpty() }?.let {
                                append("\n")
                                append("错误：")
                                append(it)
                            }
                        }.toString()
                    }
                }
                override fun getItemCount(): Int =result.size+1
            }
        }
        BottomSheetDialog(this).let {
            it.setContentView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            it.show()
            it.setOnDismissListener {
                try {
                    CleaningAndroidService.intent(this).let {
                        applicationContext.stopService(it)
                    }
                } catch (e:Exception) {
                    Log.i(Application.TAG, "onReceive: $e")
                    dialog(title = "学术不精导致的错误",message = e.toString())
                }
                finish()
            }

        }
    }
}