package me.heizi.box.package_manager.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

sealed class DialogBtns(val text:String,val icon:Drawable?=null,val onClick:DialogInterface.OnClickListener) {
    class Positive(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
    class Negative(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
    class Neutral(text: String, icon: Drawable?=null, onClick: DialogInterface.OnClickListener) :DialogBtns(text, icon, onClick)
}

/**
 * Dialog makers
 */
@MainThread
fun Context.dialog(
    title:String?=null,
    view: View?=null,
    message:String?=null,
    cancelable: Boolean = true,
    show:Boolean = true,
    vararg btns: DialogBtns
) =
    AlertDialog.Builder(this).run {
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
        if (show) show() else create()
    }


fun Context.longToast(message: String)
    = Toast.makeText(this,message,Toast.LENGTH_LONG).show()

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