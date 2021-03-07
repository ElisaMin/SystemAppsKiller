package me.heizi.box.package_manager.utils

import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.heizi.kotlinx.android.ClickBinding
import me.heizi.kotlinx.android.TextViewTextBinding

fun ViewBinding.longSnackBar(message: String) {
    GlobalScope.launch(Main) {
        Snackbar.make(root,message, Snackbar.LENGTH_LONG).show()
    }
}

fun ViewBinding.clickSnackBar(message: String, actionName:String="晓得了", onClick:(View)->Unit) {
    GlobalScope.launch(Main) {
        Snackbar.make(root,message, Snackbar.LENGTH_INDEFINITE).setAction(actionName,onClick).show()
    }
}
fun RecyclerView.ViewHolder.bindText(@IdRes id: Int) = TextViewTextBinding(itemView,id)
fun RecyclerView.ViewHolder.bindClick(@IdRes id: Int) = ClickBinding(itemView,id)
