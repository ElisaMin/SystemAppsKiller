package me.heizi.box.package_manager.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog

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
        btns.takeIf { it.isNotEmpty() }?.let {
            var i = false
            var j = false
            var k = false
            for (dialogBtn in it) {
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

