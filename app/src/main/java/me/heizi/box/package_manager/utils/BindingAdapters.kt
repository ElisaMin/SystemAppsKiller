package me.heizi.box.package_manager.utils

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView

@BindingAdapter("android:visibility",requireAll = false)
fun visibility(view:View,boolean: Boolean) {
    view.isVisible = boolean
}

@BindingAdapter("android:adapter")
fun recyclerViewAdapting(recyclerView: RecyclerView,adapter:RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
    recyclerView.adapter = adapter
}