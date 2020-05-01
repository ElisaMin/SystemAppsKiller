package me.heizi.box.packagemanager


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
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
import me.heizi.box.packagemanager.listmaker.MyRecyclerViewAdapter
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.zip.Inflater
import kotlin.concurrent.thread

lateinit var recyclerView:RecyclerView
class MainActivity : AppCompatActivity() {

    companion object{
        lateinit var sharedPreferences: SharedPreferences
        lateinit var context: Context
        var isBackup:Boolean=false
    }
    fun mkaMessage(string: String){
        Toast.makeText(this,string,Toast.LENGTH_LONG).show()
    }
    val TAG = "Main"


    fun rootCheck(): Unit {
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setView(ProgressBar(this))
            .show()
        try {
            ShellUtils.execCmd("echo HelloWorld",true,true).let{
                if (it.isSuccess){
                    loadingDialog.hide()
                }else{
                    loadingDialog.hide()
                    MaterialAlertDialogBuilder(this)
                        .setTitle("无root")
                        .setMessage("本应用不适用于无Root设备")
                        .show()
                }
            }
        }catch (e:Exception){
            mkaMessage("$e")
        }
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
        rootCheck()
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


