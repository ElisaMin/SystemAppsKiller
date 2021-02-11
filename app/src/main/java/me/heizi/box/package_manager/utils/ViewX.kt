package me.heizi.box.package_manager.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

sealed class DialogBtns(val text:String,val icon:Drawable?=null,val onClick:DialogInterface.OnClickListener) {
    class Positive(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
    class Negative(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
    class Neutral(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
}

/**
 * Dialog makers
 */
fun Context.dialogBuilder(
    vararg btns: DialogBtns,
    title:String?=null,
    view: View?=null,
    message:String?=null,
    cancelable: Boolean = true,
) =
    AlertDialog.Builder(this).apply {
        setTitle(title)
        setView(view)
        setMessage(message)
        setCancelable(cancelable)
        btns.takeIf { it.isNotEmpty() }?.let { btns ->
            var i = false
            var j = false
            var k = false
            for (dialogBtn in btns) {
                if (i||j||k) throw IllegalArgumentException("only one type button on same dialog")
                when(dialogBtn) {
                    is DialogBtns.Positive -> {
                        dialogBtn.icon?.let { setPositiveButtonIcon(it) }
                        setPositiveButton(dialogBtn.text,dialogBtn.onClick)
                        i = true
                    }
                    is DialogBtns.Negative -> {
                        dialogBtn.icon?.let { setNegativeButtonIcon(it) }
                        setNegativeButton(dialogBtn.text,dialogBtn.onClick)
                        j = true
                    }
                    is DialogBtns.Neutral -> {
                        dialogBtn.icon?.let { setNeutralButtonIcon(it) }
                        setNeutralButton(dialogBtn.text,dialogBtn.onClick)
                        k = true
                    }
                }
            }
        }
    }
/**
 * Dialog makers
 */
fun Context.dialog(
    vararg btns: DialogBtns,
    title:String?=null,
    view: View?=null,
    message:String?=null,
    cancelable: Boolean = true,
    show:Boolean = true,
) = dialogBuilder(*btns,title = title, view = view, message = message,cancelable =  cancelable).run {
    if (show) show() else create()
}


fun Context.longToast(message: String)
    = Toast.makeText(this,message,Toast.LENGTH_LONG).show()
fun Context.shortToast(message: String)
    = Toast.makeText(this,message,Toast.LENGTH_SHORT).show()

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
class TextViewTextBinding (
    parent: View,
    @IdRes private val id:Int
) {
    private val view:TextView? by lazy { parent.findViewById(id) }
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return view?.text?.toString()
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        view?.text = value
    }
}
class ClickBinding (
    parent: View,
    @IdRes private val id:Int
) {
    private val view:View? by lazy { parent.findViewById(id) }
    operator fun getValue(thisRef: Any?, property: KProperty<*>): (()->Unit)? {
        return null
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, block:(()->Unit)?)  {
        view?.setOnClickListener { block?.invoke() }
    }
}