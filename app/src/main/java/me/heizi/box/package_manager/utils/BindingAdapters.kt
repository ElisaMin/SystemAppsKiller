package me.heizi.box.package_manager.utils

import android.view.View
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

@BindingAdapter("android:visibility",requireAll = false)
fun visibility(view:View,boolean: Boolean) {
    view.isVisible = boolean
}