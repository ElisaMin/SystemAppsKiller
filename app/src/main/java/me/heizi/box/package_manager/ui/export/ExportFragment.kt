package me.heizi.box.package_manager.ui.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.heizi.box.package_manager.R
import me.heizi.box.package_manager.databinding.ExportFragmentBinding

class ExportFragment : BottomSheetDialogFragment() {
    private val binding by lazy { ExportFragmentBinding.bind(requireView()) }
    private val viewModel:ExportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.export_fragment,container)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
    }

}