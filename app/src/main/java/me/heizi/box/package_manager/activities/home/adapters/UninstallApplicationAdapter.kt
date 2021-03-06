package me.heizi.box.package_manager.activities.home.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.databinding.ItemAppUninstallWithTitleBinding
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.isUserApp
import me.heizi.box.package_manager.utils.*
import me.heizi.box.package_manager.utils.PathFormatter.diffPreviousPathAreNotSame
import me.heizi.kotlinx.android.DialogBtns
import me.heizi.kotlinx.android.dialog
import me.heizi.kotlinx.android.longToast
import me.heizi.kotlinx.android.shortToast
import java.util.*


/**
 * New adapter
 *
 * 如果它只储存ViewHolder那就实时渲染好
 */
@SuppressLint("NotifyDataSetChanged")
class UninstallApplicationAdapter constructor(
    private val scope: CoroutineScope,
        private val service: Service,
        private val processing:()->Unit,
        private val stopProcessing:()->Unit
) :RecyclerView.Adapter<UninstallApplicationAdapter.ViewHolder>(),Filterable {

    interface Service {
        val allAppF: StateFlow<MutableList<ApplicationInfo>>
        val allApps get() =  allAppF.value
        fun removeItemFormAllApps(applicationInfo: ApplicationInfo) = allAppF.value.remove(applicationInfo)
        fun getAppLabel(applicationInfo: ApplicationInfo):String
        fun getAppIcon(applicationInfo: ApplicationInfo): Drawable?
        fun getPrevPath(applicationInfo: ApplicationInfo):String
        fun uninstall(applicationInfo: ApplicationInfo, position: Int)
    }


    init {
        scope.launch(Dispatchers.Unconfined) {
            stopProcessing()
            service.allAppF.collectLatest {
                processing()
                launch(Main) {
                    current=ArrayList(service.allApps)
                    notifyDataSetChanged()
                    stopProcessing()
                }
            }
        }
    }

    /**
     * Current
     *
     * 正在显示数据的List
     */
    private var current:ArrayList<ApplicationInfo> = ArrayList(service.allApps)

    /**
     * Submit list
     *
     * 更新List without notify
     */
    private fun submitList(list:List<ApplicationInfo>) {current = ArrayList(list)}

    /**
     * Remove at
     *
     * 删除页面中正在显示的item 并通知更新
     */
    fun removeAt(position: Int) {
        val item = getItem(position)
        if(service.removeItemFormAllApps(item))
        if (current.remove(item)) scope.launch(Main) {
            notifyItemRemoved(position)
            notifyItemRangeChanged(position-1,3)
        }
    }

    /**
     * 返回一个正在显示中的ApplicationInfo
     */
    private fun getItem(position: Int) = current[position]

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        = ItemAppUninstallWithTitleBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
            .let(UninstallApplicationAdapter::ViewHolder)
            .also { scope.launch { it.bind(viewType) } }


    private suspend fun ViewHolder.bind(position: Int) {
        var title:String? = null
        Log.i(TAG, "bind: $position")
        val item = current[position]
        val now = service.getPrevPath(item)
        Log.i(TAG, "bind: ${item.packageName}")
        if (position == 0) {
            title = now
        } else {
            val prev = service.getPrevPath(current[position - 1])
            val notSame = now.diffPreviousPathAreNotSame(prev)
            if (notSame) title = now
        }
        val name = service.getAppLabel(item)
        val viewModel = ViewHolder.ViewModel(title, name, item.sourceDir)
        binding.viewModel = viewModel
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i(TAG, "onBindViewHolder: $position")
        holder.binding.run {
            scope.launch (Main) {
                val item = getItem(position)
                val icon = service.getAppIcon(item)
                uninstallBtn.setOnClickListener {
                    onUninstallCalling(position,root.context)
                }
                iconAppView.setImageDrawable(icon)
                if (viewModel!!.binding == true ) {
                    Log.i(TAG, "onBindViewHolder: updating Info")
                    holder.bind(position)
                }

            }
            if (viewModel!!.binding == null) viewModel!!.binding = true
        }
    }

    private fun onUninstallCalling(position: Int,context: Context) {
        val applicationInfo = getItem(position)
        //判断是否为系统应用
        val isSystemApp = !applicationInfo.isUserApp
        //开始卸载
        if (isSystemApp) context.dialog (
            DialogBtns.Positive("卸载") { _, _->
                processing()
                try {
                    service.uninstall(applicationInfo, position)
                } catch (e:Uninstall.NotNormallyPath) {
                    context.dialog(title ="路径超出范围",message = e.message)
                    stopProcessing()
                } catch (e:Uninstall.NotSystemApp) {
                    try {
                        context.shortToast("非系统应用,请手动卸载")
                        TODO("跳转到app详细页面未实现")
                    }catch (e:Exception) {
                        context.longToast(e.message?:e.toString())
                    }
                    stopProcessing()
                }catch (e:Exception) {
                    context.dialog(title = "未知错误",message = e.message)
                    stopProcessing()
                }
            },
            title = "是否需要卸载?",
            message = "卸载本应用可能会造成系统不稳定,确认要卸载吗?",
        ) else {
            context.longToast("下次再添加卸载普通应用的功能哈哈")
        }
    }

    override fun getItemCount(): Int = current.size

    private val filter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                (if (constraint.isNullOrEmpty()) service.allApps
                else constraint.toString().let {s->
                    val keys = s.trim().toLowerCase(Locale.ENGLISH).split(" ","\n","/")
                    if (keys.size>1) {
                        //整上regex 匹配 .*(模糊)+.*(文字)+.*(文字)+.*
                        val stringBuilder = StringBuilder()
                        stringBuilder.append(".*(")
                        for (k in keys) {
                            stringBuilder.append(")+.*(")
                            stringBuilder.append(k)
                        }
                        stringBuilder.append(")+.*")
                        val regex = stringBuilder.toString().toRegex()
                        service.allApps.filter {
                            val names = service.getAppLabel(it).trim().toLowerCase(Locale.ENGLISH).split(" ")
                            for (n in names) if (regex.matches(n)) return@filter true
                            val paths = it.sourceDir.trim().toLowerCase(Locale.CHINA).split(".", "/", "_", "-")
                            for(p in paths) if (regex.matches(p)) return@filter true
                            false
                        }
                    }else {
                        service.allApps.filter {
                            service.getAppLabel(it).contains(s) || it.sourceDir.contains(s)
                        }
                    }
                }).let(::submitList)
                return FilterResults()
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }

        }
    }

    override fun getFilter(): Filter = filter

    class ViewHolder(val binding:ItemAppUninstallWithTitleBinding):RecyclerView.ViewHolder(binding.root) {
        data class ViewModel(
            val title:String? = null,
            val name: String = "加载失败",
            val sDir:String,
            var binding: Boolean? = null
        ) {
            val isShowingTitle get() = title != null
        }

    }
}