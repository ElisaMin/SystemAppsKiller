package me.heizi.box.package_manager.utils

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("android:visibility",requireAll = false)
fun visibility(view:View,boolean: Boolean) {
    view.isVisible = boolean
}

@BindingAdapter("android:adapter")
fun recyclerViewAdapting(recyclerView: RecyclerView,adapter:RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
    recyclerView.adapter = adapter
}
@BindingAdapter("android:onClick")
fun onClickAdapter(view: View,onClick:(()->Unit)?) {
    view.setOnClickListener { onClick?.invoke() }
}
@BindingAdapter(
//    "android:endIconOnClick",
    "android:startIconOnClick",requireAll = false)
fun onInputIconClickAdapter(view: TextInputLayout,onClick:(()->Unit)?) {
    view.setStartIconOnClickListener { onClick?.invoke() }
}