package me.heizi.box.package_manager.activities.home.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.activities.home.HomeActivity.Companion.parent
import me.heizi.box.package_manager.activities.home.adapters.EditUninstallListAdapter
import me.heizi.box.package_manager.custom_view.BottomSheetDialogFragment
import me.heizi.box.package_manager.databinding.DialogListEditBinding
import me.heizi.box.package_manager.models.UninstallInfo
import me.heizi.box.package_manager.utils.default
import me.heizi.box.package_manager.utils.main

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
        viewModel.adapter.submitList(sourceList)
        Log.i(TAG, "onViewCreated: source size ${sourceList.size}")
        binding.viewModel = viewModel
        binding.lifecycleOwner = parent
        binding.btnSssssssssss.setOnClickListener { viewModel.onDoneClicked() }

    }
    inner class ViewModel {
        val adapter by lazy {
            object : EditUninstallListAdapter() {
                override fun onRemoveBtnClicked(position: Int) = default {
                    if (beforeItemRemove(this, getItem(position))) super.onRemoveBtnClicked(position)
                    main { notifyDataSetChanged() }
                }
            }
        }
        val name = MutableStateFlow(defaultName)
        fun onDoneClicked() = default {
            if(onDone(adapter.currentList,name.value)) dismiss()
        }
    }
}