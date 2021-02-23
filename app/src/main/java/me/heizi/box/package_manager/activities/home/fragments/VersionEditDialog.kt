package me.heizi.box.package_manager.activities.home.fragments

import android.os.Bundle
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import me.heizi.box.package_manager.activities.home.adapters.EditUninstallListAdapter
import me.heizi.box.package_manager.custom_view.BottomSheetDialogFragment
import me.heizi.box.package_manager.databinding.DialogListEditBinding
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.default

class VersionEditDialog(
    private val sourceList: ArrayList<UninstallInfo>,
    private val defaultName:String,
    private val beforeItemRemove:suspend CoroutineScope.(UninstallInfo)->Boolean,
    private val onDone:(final:List<UninstallInfo>,String)->Boolean,
): BottomSheetDialogFragment<DialogListEditBinding>()  {
    override val binding by lazy {DialogListEditBinding.inflate(layoutInflater)}
    private val viewModel = ViewModel()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
    }
    inner class ViewModel {
        val adapter by lazy {
            object : EditUninstallListAdapter() {
                override fun onRemoveBtnClicked(position: Int) = default {
                    if (beforeItemRemove(this, getItem(position))) super.onRemoveBtnClicked(position)
                }
            }.also { it.submitList(sourceList) }
        }
        val name = MutableStateFlow(defaultName)
        fun onDoneClicked() = default {
            if(onDone(adapter.currentList,name.value)) dismiss()
        }
    }
}