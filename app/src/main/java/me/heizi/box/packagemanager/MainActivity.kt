package me.heizi.box.packagemanager


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dev.utils.app.ShellUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.heizi.box.packagemanager.libs.defaultUninstallList as DEFAULT_UNINSTALL_LIST_29D
import me.heizi.box.packagemanager.libs.fall
import me.heizi.box.packagemanager.libs.success
import me.heizi.box.packagemanager.listmaker.MyRecyclerViewAdapter
import java.io.IOException
import java.lang.StringBuilder
import kotlin.Exception
import kotlin.concurrent.thread
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val TAG = "Main"

    /*
    环境检查：
    检查是否为ROOT环境 实现!
    分配系统的挂载方式
     */

    fun checkRoot() /*= runBlocking*/ {// 停下所有工作
        var loadingDialog: AlertDialog? = null
        //io携程内设置dialog handel不了
        //GlobalScope.launch(Dispatchers.IO) {
            loadingDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setView(ProgressBar(this@MainActivity))
                .show()
        //}
        //默认携程内检测Root
        //GlobalScope.launch(Dispatchers.Default) {
            try {
                ShellUtils.execCmd("echo FuckTheWorld", true, true)

                    .success {
                        checkSystem(loadingDialog!!)
                    }

                    .fall {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("无root")
                            .setMessage("本应用不适用于无Root设备")
                            .show()
                    }

            } catch (e: Exception) {
                //一般来说会出现 su not found之类的错误
                mkaMessage("$e")
            }
        //}
    }

    fun checkSystem(dialog: AlertDialog) /*= runBlocking*/ {
        //在我会Mount 9.0之前 这个功能属于扯淡
        dialog.hide()
    }

    lateinit var laodingDialogInUinstall:AlertDialog

    fun uninstallFromShare(string: String): Unit {
        laodingDialogInUinstall.show()
        for ((i,dir) in string.lines().withIndex()) {
            uninstallApp(dir)
            if (i==string.lines().size-2) laodingDialogInUinstall.hide()
        }
    }
    var i =0
    fun uninstallApp(string: String): Boolean {
        ShellUtils.execCmd(arrayOf("mount -wo remount /oem/OP ","chmod 777 $string"),true)
        ShellUtils.execCmd(arrayOf("mount -wo remount / ","chmod 777 $string"),true)
        return ShellUtils.execCmd(if (!isBackup) "rm -rf $string" else " mkdir -p /sdcard$string && mv $string /sdcard$string ",  true, false).apply {
            Snackbar.make(recyclerView,"$string   运行中x${i++}",Snackbar.LENGTH_SHORT).show()
        }.isSuccess

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        //赋值
        context                                    =    this
        sharedPreferences                          =    getSharedPreferences("AppShit",Context.MODE_PRIVATE)
        me.heizi.box.packagemanager.packageManager =    this.packageManager
        //检查是否需要备份
        try {
            // 成功会获得true
            isBackup                               = getPreferences(Context.MODE_PRIVATE).getBoolean("back_up",false)
        }catch (e:Exception){
            e.printStackTrace()
            //找不到时会报错
            getPreferences(Context.MODE_PRIVATE).edit().run {
                putBoolean("back_up",false)
                apply()
            }
        }

        //检查root
        checkRoot()
        //启动时
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logi("now is view loaded")
        //toolbar绑定
        setSupportActionBar(toolbar)
        //recyclerView 初始化
        updateRecyclerView()
    }

    fun updateRecyclerView(){
        //包级变量赋值
        me.heizi.box.packagemanager.recyclerView = recyclerView
        //下面一段都是抄网上的
        val packs=packageManager.getInstalledPackages(0)
        val userAppList:ArrayList<PackageInfo> = ArrayList()
        val systemAppList:ArrayList<PackageInfo> =ArrayList()
        for ( pkginfo in packs) {
            pkginfo.let {
                if (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM <= 0)
                    userAppList.add(it)
                else
                    systemAppList.add(it)
            }
            //Log.e("shit",packs[0].applicationInfo.dataDir)
            //绑定Adapter
            recyclerView.adapter = MyRecyclerViewAdapter(systemAppList)
            recyclerView.layoutManager=LinearLayoutManager(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu,menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.deleteFromText ->  {
                val editText:TextInputLayout = layoutInflater.inflate(R.layout.input_delete,null) as TextInputLayout

                laodingDialogInUinstall = MaterialAlertDialogBuilder(this)
                    .setPositiveButton("确认"){ _: DialogInterface, _: Int ->
                        thread { uninstallFromShare(editText.editText!!.text.toString()) }
                    }
                    .setNeutralButton("29d默认列表(先解OP)"){_,_->
                        uninstallFromShare(DEFAULT_UNINSTALL_LIST_29D)
                    }
                    .setTitle("导入卸载列表")
                    .setView(editText)
                    .show()

            }
            R.id.setting -> {
                val insideView=layoutInflater.inflate(R.layout.setting_dialog,null) as ConstraintLayout
                val bus=insideView.getViewById(R.id.backup_switcher) as Switch
                bus.isChecked= isBackup
                MaterialAlertDialogBuilder(this)
                    .setView(insideView)
                    .setTitle("设置")
                    .show()

                bus.setOnCheckedChangeListener { button, isChecked -> isBackup =isChecked
                    getPreferences(Context.MODE_PRIVATE).edit().let {
                        it.putBoolean("back_up",isChecked)
                        it.apply()
                    }
                }
            }
            R.id.seeIt -> {
                val all = sharedPreferences.all
                val string : StringBuilder=StringBuilder()
                for (i in all) {
                    string.append("${i.value}\n")
                }

                val textView =TextView(this)

                textView.let {
                    it.setTextIsSelectable(true)
                    it.text=string
                    it.setPadding(8)
                }

                MaterialAlertDialogBuilder(this)
                    .setView(textView)
                    .show()
            }
        }
        return true
    }


}


