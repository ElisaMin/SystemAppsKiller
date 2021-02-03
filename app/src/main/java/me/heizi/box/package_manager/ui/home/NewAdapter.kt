package me.heizi.box.package_manager.ui.home

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.databinding.ItemAppUninstallWithTitleBinding
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.diffPreviousPathAreNotSame
import me.heizi.box.package_manager.repositories.PackageRepository.Companion.getPreviousPath

/**
 * New adapter
 *
 * 如果它只储存ViewHolder那就实时渲染好
 */
class NewAdapter constructor(
    private val pm:PackageManager,
    private val scope: CoroutineScope,
    private val appFlow:StateFlow<List<ApplicationInfo>>,
    private val onUninstall:(Int)->Unit
) :RecyclerView.Adapter<NewAdapter.ViewHolder>() {


    private val current:List<ApplicationInfo> get() = appFlow.value

    class ViewHolder(val binding:ItemAppUninstallWithTitleBinding):RecyclerView.ViewHolder(binding.root) {

        data class ViewModel(
            val title:String? = null,
            val name: String = "加载失败",
            val icon: Drawable,
            val sDir:String,
        ) {
            val isShowingTitle get() = title != null
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        = ItemAppUninstallWithTitleBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
            .let(::ViewHolder)


    override fun onBindViewHolder(holder: ViewHolder, position: Int):Unit { scope.launch(Default){
        var title:String? = null
        val item = current[position]
        val now = getPreviousPath(item.sourceDir)
        if (position == 0) {
            title = now
        } else {
            val prev = getPreviousPath(current[position-1].sourceDir)
            val notSame = now.diffPreviousPathAreNotSame(prev)
            if (notSame) title = prev
        }
        val icon = pm.getApplicationIcon(item)
        val name = pm.getApplicationLabel(item).toString()
        val viewModel = ViewHolder.ViewModel(title,name,icon,item.sourceDir)
        holder.binding.viewModel = viewModel
        holder.binding.uninstallBtn.setOnClickListener {
            onUninstall(position)
        }
    }}

    override fun getItemCount(): Int = current.size
}