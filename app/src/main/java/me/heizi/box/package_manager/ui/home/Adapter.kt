package me.heizi.box.package_manager.ui.home

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.databinding.ItemAppUninstallWithTitleBinding
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.diffPreviousPathAreNotSame
import java.util.*
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.labels as label
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.prevPathIndexed as indexed


/**
 * New adapter
 *
 * 如果它只储存ViewHolder那就实时渲染好
 */
class Adapter constructor(
    private val pm:PackageManager,
    private val scope: CoroutineScope,
    private val appFlow:StateFlow<MutableList<ApplicationInfo>>,
    private val onUninstall:(Int)->Unit
) :RecyclerView.Adapter<Adapter.ViewHolder>(),Filterable {




    private var current:ArrayList<ApplicationInfo> = ArrayList(appFlow.value)

    private fun submitList(list:List<ApplicationInfo>) {current = ArrayList(list)}

    fun removeAt(position: Int) {
        Log.i(TAG, "removeAt: starting remove $position")
        val item = getItem(position)
        Log.i(TAG, "removeAt: ${item.packageName}")
        if(appFlow.value.remove(item)) {
            Log.i(TAG, "removeAt: flow values is already removed")
        }
        if (current.remove(item)) {
            Log.i(TAG, "removeAt: current dose")
            scope.launch(Main) {
                Log.i(TAG, "removeAt: notifying")
                notifyItemRemoved(position)
                notifyItemRangeChanged(position-1,position+1)
            }
        }


    }

    fun getItem(position: Int) = current[position]

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        = ItemAppUninstallWithTitleBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
            .let(::ViewHolder)
            .also { scope.launch { it.bind(viewType) } }


    private suspend fun ViewHolder.bind(position: Int) {
        var title:String? = null
        Log.i(TAG, "bind: $position")
        val item = current[position]
        val now = indexed[item.packageName]
        Log.i(TAG, "bind: ${item.packageName}")
        if (position == 0) {
            title = now
        } else {
            val prev = indexed[current[position - 1].packageName]
            val notSame = now!!.diffPreviousPathAreNotSame(prev!!)
            if (notSame) title = prev
        }
        val name = label[item.packageName] ?: pm.getApplicationLabel(item).toString()
        val viewModel = ViewHolder.ViewModel(title,name,item.sourceDir)
        binding.viewModel = viewModel

    }



    fun getApplicationPath(app:ApplicationInfo) = label[app.packageName] ?: pm.getApplicationLabel(app).toString()


    override fun onBindViewHolder(holder: ViewHolder, position: Int):Unit {
        Log.i(TAG, "onBindViewHolder: $position")
        holder.binding.run {
            scope.launch (Main) {
                val item = getItem(position)
                val icon = pm.getApplicationIcon(item)
                uninstallBtn.setOnClickListener {
                    onUninstall(position)
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
    override fun getItemCount(): Int = current.size






    private val filter by lazy {
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                (if (constraint.isNullOrEmpty()) appFlow.value
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
                        appFlow.value.filter {
                            val names = label[it.packageName]!!.trim().toLowerCase(Locale.ENGLISH).split(" ")
                            for (n in names) if (regex.matches(n)) return@filter true
                            val paths = it.sourceDir.trim().toLowerCase(Locale.CHINA).split(".", "/", "_", "-")
                            for(p in paths) if (regex.matches(p)) return@filter true
                            false
                        }
                    }else {
                        appFlow.value.filter {
                            label[it.packageName]?.contains(s) == true || it.sourceDir.contains(s)
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