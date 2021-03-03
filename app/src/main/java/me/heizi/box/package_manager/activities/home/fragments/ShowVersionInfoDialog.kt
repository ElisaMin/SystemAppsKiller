package me.heizi.box.package_manager.activities.home.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import me.heizi.box.package_manager.activities.home.HomeActivity.Companion.parent
import me.heizi.box.package_manager.custom_view.BottomSheetDialogFragment
import me.heizi.box.package_manager.dao.entities.Version
import me.heizi.box.package_manager.databinding.DialogShowQcCodeBinding
import me.heizi.box.package_manager.utils.DialogBtns
import me.heizi.box.package_manager.utils.dialog

class ShowVersionInfoDialog <Image>(
    private val viewModel:ViewModel<Image>
):BottomSheetDialogFragment<DialogShowQcCodeBinding>() {
    override val binding: DialogShowQcCodeBinding by lazy { DialogShowQcCodeBinding.inflate(layoutInflater) }
    private val editText by lazy{ EditText(context) }
    private val dialog by lazy { context?.dialog(DialogBtns.Positive("确定") {_,_-> },view = editText,title = "输入文字") }
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = parent
        binding.viewModel = viewModel
        (binding.root as ViewGroup).children
            .filterIsInstance(ImageView::class.java)
            .forEach {
                when(val g = viewModel.bitmap) {
                    is Bitmap   -> it.setImageBitmap(g)
                    is Drawable -> it.setImageDrawable(g)
                }
            }
        binding.editable.text = "本列表一共"+viewModel.listSize+"个应用，"+"此段文字可编辑。"
        editText.doOnTextChanged { text, _, _, _ ->
            (text?.takeIf { it.isNotEmpty() }?.toString() ?: "此段文字可编辑。").let {
                binding.editable.text = "本列表一共"+viewModel.listSize+"个应用，"+it
            }
        }
        binding.editable.setOnClickListener {
            dialog?.show()
        }
        val callEdit = viewModel.editBtnClick
        viewModel.editBtnClick = {
            callEdit.invoke()
            this.dismiss()
        }
    }
    class ViewModel<Image>(
        vararg btn:Pair<String,()->Unit>,
        val bitmap: Image?,
        listSize: Int,
        val version:Version,
        val helpText:String? = null,
    ) {
        val listSize = listSize.toString()
        val editBtnText = btn[0].first
        var editBtnClick = btn[0].second
        val copyBtnText = btn[1].first
        val copyBtnClick = btn[1].second
    }
}