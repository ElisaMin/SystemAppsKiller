package me.heizi.box.packagemanager


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.nfc.Tag
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
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dev.utils.app.ShellUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.input_delete.*
import kotlinx.android.synthetic.main.setting_dialog.*
import kotlinx.android.synthetic.main.setting_dialog.view.*
import kotlinx.coroutines.*
import me.heizi.box.packagemanager.libs.fall
import me.heizi.box.packagemanager.libs.success
import me.heizi.box.packagemanager.listmaker.MyRecyclerViewAdapter
import java.io.IOException
import java.lang.StringBuilder
import java.util.zip.Inflater
import kotlin.Exception
import kotlin.concurrent.thread
// 包级变量
lateinit var recyclerView:RecyclerView
lateinit var sharedPreferences: SharedPreferences
lateinit var context: Context
lateinit var packageManager: PackageManager
var isBackup:Boolean=false
var mountSystem = arrayOf("mount -wo remount / ","chmod 777 /")
var mountOP = arrayOf("mount -wo remount /oem/OP","chmod 777 /oem/OP")
//包级函数

suspend fun uninstallSystemApp(applicationInfo: ApplicationInfo):Boolean {
    val editor = sharedPreferences.edit()
    var result:Deferred<Boolean>? = null
    var dialogLoading:AlertDialog? = null

    runBlocking {
        launch(Dispatchers.IO) {
            dialogLoading = MaterialAlertDialogBuilder(context)
                .setView(ProgressBar(context))
                .show()
        }
        result = async (Dispatchers.Default) {
            return@async try {
                //获取所需变量
                val label = packageManager.getApplicationLabel(applicationInfo)
                val apkPath:String   = applicationInfo.sourceDir!!.replace("/[A-Za-z0-9 -]+.apk".toRegex(), "")
                val dataPath:String? = if (arrayOf("","").contains(applicationInfo.dataDir)) null else applicationInfo.dataDir!!
                // 开始删除
                ShellUtils.execCmd(
                    ArrayList<String>().apply {
                        //判断挂载分区
                        addAll(if (apkPath.split("/").contains("OP")) {mountOP} else {mountSystem})
                        //接上
                        addAll(
                            arrayOf(
                                "chmod 777 $apkPath",
                                if (!isBackup)  {"rm -rf $apkPath"}
                                else            {"mkdir -p /sdcard/appsBackup$apkPath && mv $apkPath /sdcard/appsBackup$apkPath"}
                            )
                        )
                    }.toTypedArray(),      //commands
                    true,          //isRoot
                    true  //isNeedResultMessage
                ) success {
                    editor.run {
                        putString("${label}_source",apkPath)
                        apply()
                    }
                    dataPath?.let { p->
                        if (ShellUtils.execCmd("rm -rf '$p'", true, true).isSuccess){
                            editor.run {
                                putString("${label}_data", p)
                                apply()
                            }
                        }
                    }
                    mkaMessage("成功")
                } fall {
                    mkaMessage("失败，原因：\n${this.errorMsg}")
                }
                true
            }catch (e:IOException){

                mkaMessage(e.toString())
                false
            }catch (e:java.lang.Exception){
                Log.e("Uninstalling",e.toString(),e)
                false
            }finally {
                dialogLoading!!.hide()
            }
        }
    }
    return result!!.await()
}
fun mkaMessage(string: String){
    Toast.makeText(context,string,Toast.LENGTH_LONG).show()
}
class MainActivity : AppCompatActivity() {

    val TAG = "Main"
    //Toast Maker

    /*
    环境检查：
    检查是否为ROOT环境 实现!
    分配系统的挂载方式
     */

    fun checkRoot() = runBlocking {// 停下所有工作
        var loadingDialog: AlertDialog? = null
        //io携程内设置dialog
        launch(Dispatchers.IO) {
            loadingDialog = MaterialAlertDialogBuilder(this@MainActivity)
                .setView(ProgressBar(this@MainActivity))
                .show()
        }
        //默认携程内检测Root
        launch(Dispatchers.Default) {
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
        }
    }

    fun checkSystem(dialog: AlertDialog) = runBlocking {
        //在我会Mount 9.0之前 这个功能属于扯淡

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

    fun updateRecyclerView(){
        me.heizi.box.packagemanager.recyclerView = recyclerView
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
            Log.e("shit",packs[0].applicationInfo.dataDir)
            recyclerView.adapter = MyRecyclerViewAdapter(systemAppList)
            recyclerView.layoutManager=LinearLayoutManager(this)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context=this
        sharedPreferences =getSharedPreferences("AppShit",Context.MODE_PRIVATE)
        me.heizi.box.packagemanager.packageManager = this.packageManager

        try {
            isBackup=getPreferences(Context.MODE_PRIVATE).getBoolean("back_up",false)
        }catch (e:Exception){
            e.printStackTrace()
            getPreferences(Context.MODE_PRIVATE).edit().let {
                it.putBoolean("back_up",false)
                it.apply()
            }
        }

        if (getPreferences(Context.MODE_PRIVATE).getBoolean("back_up",false)){

        }
        super.onCreate(savedInstanceState)
        checkRoot()
        setContentView(R.layout.activity_main)

        loge("now is view loaded")

        setSupportActionBar(toolbar)
        updateRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater!!.inflate(R.menu.main_toolbar_menu,menu)
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
                        uninstallFromShare("/oem/OP/priv-app/LGU_KR/LGAssistant\n" +
                                "/system/product/priv-app/CollageWallpapers\n" +
                                "/system/product/priv-app/MyPlacesEngine\n" +
                                "/data/user/0/com.lge.qlens\n" +
                                "/data/user/0/com.google.android.apps.docs.editors.slides\n" +
                                "/data/user/0/com.google.android.youtube\n" +
                                "/data/user_de/0/com.lge.myplace\n" +
                                "/system/product/priv-app/LGLW_MultiPhoto/LGLW_MultiPhoto.apk\n" +
                                "/data/user/0/com.lge.gallery.vr.wallpaper\n" +
                                "/data/user/0/com.google.android.music\n" +
                                "/system/product/priv-app/Facebook_Service/Facebook_Service.apk\n" +
                                "/data/user/0/com.lge.lgassistant\n" +
                                "/data/user/0/com.google.ar.lens\n" +
                                "/data/user/0/com.google.android.apps.maps\n" +
                                "/system/app/EditorsSlides\n" +
                                "/data/user_de/0/com.lge.ime\n" +
                                "/system/app/Drive\n" +
                                "/data/user/0/com.lge.video.vr.wallpaper\n" +
                                "/system/app/EditorsSheets\n" +
                                "/data/user/0/com.facebook.appmanager\n" +
                                "/system/app/Maps\n" +
                                "/data/user/0/com.lge.iuc\n" +
                                "/data/user/0/com.lge.vrplayer\n" +
                                "/data/user/0/com.google.android.apps.docs\n" +
                                "/system/product/priv-app/LGMapUI\n" +
                                "/system/app/LensStub\n" +
                                "/data/user/0/com.google.android.videos\n" +
                                "/data/user/0/com.google.android.apps.tachyon\n" +
                                "/system/product/priv-app/LGVRPlayer\n" +
                                "/data/user/0/com.google.android.gm\n" +
                                "/system/product/priv-app/LGLiveWallpapersPicker\n" +
                                "/data/user/0/com.android.wallpaper.livepicker\n" +
                                "/system/priv-app/LGEIME\n" +
                                "/system/product/app/Facebook_AppManager/Facebook_AppManager.apk\n" +
                                "/oem/OP/priv-app/LGU_KR/QLens\n" +
                                "/data/user/0/com.lge.tdmb\n" +
                                "/data/user/0/com.lge.lgpay\n" +
                                "/oem/OP/priv-app/LGU_KR/ONEStoreClient\n" +
                                "/system/app/Music2\n" +
                                "/system/app/EditorsDocs\n" +
                                "/system/product/priv-app/LGIUC\n" +
                                "/oem/OP/priv-app/LGU_KR/ONEStoreService\n" +
                                "/data/user/0/com.lge.lgmapui\n" +
                                "/data/user_de/0/com.lge.myplace.engine\n" +
                                "/system/app/Videos\n" +
                                "/data/user/0/com.lge.gallery.collagewallpaper\n" +
                                "/system/priv-app/LGMyPlace\n" +
                                "/data/user/0/com.lguplus.appstore\n" +
                                "/oem/OP/priv-app/LGU_KR/LGTDMB\n" +
                                "/system/priv-app/DocumentsUI\n" +
                                "/system/priv-app/LG360VideoWallpaper\n" +
                                "/data/user/0/com.android.documentsui\n" +
                                "/system/product/priv-app/LGPay\n" +
                                "/data/user/0/com.lge.livewallpaper.multiphoto\n" +
                                "/data/user/0/com.google.android.apps.docs.editors.docs\n" +
                                "/data/user/0/com.lguplus.mobile.cs\n" +
                                "/system/app/Gmail2\n" +
                                "/data/user/0/com.facebook.services\n" +
                                "/system/app/YouTube\n" +
                                "/system/app/Duo\n" +
                                "/data/user/0/com.google.android.apps.docs.editors.sheets\n" +
                                "/system/product/priv-app/LG360Wallpaper\n" +
                                "/data/user/0/com.skt.skaf.OA00018282\n" +
                                "/oem/OP/app/LGU_KR/lguplusp_mcc/lguplusp_mcc.apk\n")
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
                var string : StringBuilder=StringBuilder()
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

    fun anyway(): Unit {
    }

    fun logi(x:String): Unit {
        Log.i(TAG,x)
    }
    fun loge(x:String): Unit {
        Log.e(TAG,x)
    }
}


