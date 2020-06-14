package me.heizi.box.packagemanager

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.utils.app.ShellUtils
import dev.utils.app.ShellUtils.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.heizi.box.packagemanager.libs.*
import java.io.IOException


// 包级变量
lateinit var recyclerView: RecyclerView
lateinit var sharedPreferences: SharedPreferences
lateinit var context: Context
lateinit var packageManager: PackageManager
var isBackup:Boolean=false
var mountSystem = arrayOf("mount -wo remount / ","chmod 777 /")
var mountOP = arrayOf("mount -wo remount /oem/OP","chmod 777 /oem/OP")

//包级函数

fun uninstallSystemApp(
    applicationInfo: ApplicationInfo,
    whenDone:(ShellUtils.CommandResult.()->Unit )? = null,
    whenFall:(ShellUtils.CommandResult.()->Unit)?=null,
    whenIOException: (IOException.()->Unit)?=null,
    whenException: (java.lang.Exception.()->Unit)?=null
) :Unit = uninstallSystemApp(
    sourcePath      = applicationInfo.sourceDir,
    dataPath        = applicationInfo.dataDir,
    isDialogShow    = true,
    whenDone        = whenDone,
    whenFall        = whenFall,
    whenException   = whenException,
    whenIOException = whenIOException
)


fun uninstallSystemApp(
    sourcePath: String,
    isDialogShow:Boolean =false,
    dataPath: String? = null,
    theLabel:String? = null,
    whenDone:(ShellUtils.CommandResult.()->Unit )? = null,
    whenFall:(ShellUtils.CommandResult.()->Unit)?=null,
    whenIOException: (IOException.()->Unit)?=null,
    whenException: (java.lang.Exception.()->Unit)?=null
): Unit {
    // /system/ba/ba.apk
    val label:String = theLabel ?: sourcePath.split("/")[sourcePath.split("/").size+1]
    var dialogLoading: AlertDialog? = null
    val editor = sharedPreferences.edit()
    if (isDialogShow){
        GlobalScope.launch (Dispatchers.IO) {
            dialogLoading = MaterialAlertDialogBuilder(context)
                .setView(ProgressBar(context))
                .show()
        }
    }
    GlobalScope.launch (Dispatchers.Default) {
        try {
            // 开始删除
            ShellUtils.execCmd(
                ArrayList<String>().apply {
                    //判断挂载分区
                    addAll(if (sourcePath.split("/").contains("OP")) {mountOP} else {mountSystem})
                    //接上
                    addAll(
                        arrayOf(
                            "chmod 777 $sourcePath",
                            if (!isBackup)  {"rm -rf $sourcePath"}
                            else            {"mkdir -p /sdcard/appsBackup$sourcePath && mv $sourcePath /sdcard/appsBackup$sourcePath"}
                        )
                    )
                }.toTypedArray(),      //commands
                true,          //isRoot
                true  //isNeedResultMessage
            ) success {
                editor.run {
                    putString("${label}_source",sourcePath)
                    apply()
                }
                dataPath?.let { p->
                    if (
                        (!isBackup) and
                        (ShellUtils.execCmd("rm -rf '$p'", true, true).isSuccess)
                    ) {
                        editor.run {
                            putString("${label}_data", p)
                            apply()
                        }
                    }
                }
                whenDone?.invoke(this) ?: mkaMessage("成功")
            } fall { whenFall?.invoke(this) ?: mkaMessage("失败，原因：\n${this.errorMsg}") }
        }catch (e:IOException){
            whenIOException?.invoke(e) ?: mkaMessage(e.toString())
        }catch (e:java.lang.Exception){
            whenException?.invoke(e)  ?: Log.e("Uninstalling",e.toString(),e)
        }finally {
            dialogLoading?.hide()
        }
    }
}
fun mkaMessage(string: String){
    Toast.makeText(context,string, Toast.LENGTH_LONG).show()
}

fun Any.logi(msg:String,tag:String?=null): Unit {
    Log.i(tag ?: this.javaClass.simpleName, msg)
}
fun Any.loge(msg:String,tag:String?=null): Unit {
    Log.e(tag ?: this.javaClass.simpleName, msg)
}