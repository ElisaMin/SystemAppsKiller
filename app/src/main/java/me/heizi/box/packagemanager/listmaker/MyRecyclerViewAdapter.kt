package me.heizi.box.packagemanager.listmaker
import android.app.Person
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.utils.app.ShellUtils
import kotlinx.android.synthetic.main.item_recycler_view.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import me.heizi.box.packagemanager.*
import java.io.IOException
import java.lang.Exception

class MyRecyclerViewAdapter(arraylist: ArrayList<PackageInfo>) :Adapter<MyRecyclerViewAdapter.MyHolder> (){

    companion object{
        lateinit var array:ArrayList<PackageInfo>
    }
    init {
        array=arraylist
    }


    var isOPMsgShow=true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder { return MyHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_view,parent,false)) }

    override fun getItemCount() = array.size

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(position)

    }
    inner class MyHolder(itemView: View) : ViewHolder(itemView){

        val itemLayout=itemView.item_layout
        val nameTextView=itemView.appName
        val appIcon=itemView.appIcon
        val appInfo=itemView.appInfo
        val editor=sharedPreferences.edit()
        lateinit var pkginfo:PackageInfo
        val packageManager:PackageManager = context.packageManager

        fun bind(position:Int ){

            this.pkginfo=array[position]
            val applctInfo=pkginfo.applicationInfo

            nameTextView.text=packageManager.getApplicationLabel(applctInfo)
            appIcon.setImageDrawable(packageManager.getApplicationIcon(applctInfo))
            appInfo.text=applctInfo.sourceDir
            if (!applctInfo.enabled){
                appInfo.text  = "${appInfo.text}\n已停用"
            }
            itemView.uninstall_button.setOnClickListener {
                
                val sourceDirectory = applctInfo.sourceDir.replace("/[A-Za-z0-9 -]+.apk".toRegex(), "")
                val dataDirectory = applctInfo.dataDir
                try {
                    ShellUtils.execCmd(
                        arrayOf(
                            "mount -wo remount / ",
                            "chmod 777 /oem",
                            "mount -wo remount /oem/OP",
                            "chmod 777 /oem/OP"
                        ), true,true).apply {
                        if (!isSuccess){
                            Snackbar.make(itemView,"挂载OP失败",Snackbar.LENGTH_INDEFINITE).apply {
                                setAction("不再显示"){
                                    isOPMsgShow = false
                                }
                                if (isOPMsgShow){
                                    show()
                                }
                            }
                        }
                    }
                    ShellUtils.execCmd(arrayOf("mount -wo remount / ","chmod 777 $sourceDirectory","chmod 777 ${applctInfo.sourceDir}"),true)
                    ShellUtils.execCmd(if (!isBackup) "rm -rf $sourceDirectory" else " mkdir -p /sdcard$sourceDirectory && mv $sourceDirectory /sdcard$sourceDirectory ",  true, true).let {
                        if (it.isSuccess){
                            editor.putString("${nameTextView.text}_source",sourceDirectory)
                            editor.apply()
                            array.remove(pkginfo)
                            recyclerView.adapter?.notifyItemRemoved(position)
//                            recyclerView.adapter?.notifyItemRangeRemoved(position,array.size)
                            if ((dataDirectory !=  "/data/user_de/0/") and (dataDirectory != "/data/user/0/")){
                                    if (ShellUtils.execCmd("rm -rf '$dataDirectory'", true, true).isSuccess){
                                        editor.putString("${nameTextView.text}_data", "$dataDirectory")
                                        editor.apply()
                                    }
                                }
                            mkaMessage("成功")
                        }else{
                            mkaMessage("失败 ${it.errorMsg}")
                        }
                    }
                } catch (e:IOException){
                    mkaMessage("无Root ${e.toString()}")
                }
            }
            itemView.item_layout.setOnClickListener {
                val appinfos:String="安装路径：${applctInfo.sourceDir}\n数据路径：${applctInfo.dataDir}\n${pkginfo.versionName}@${pkginfo.packageName}"
                MaterialAlertDialogBuilder(context)
                    .setIcon(packageManager.getApplicationIcon(applctInfo))
                    .setMessage(appinfos)
                    .setTitle(nameTextView.text)
                    .show()

            }
        }

        fun mkaMessage(x: String): Unit {
            Snackbar.make(itemView,x,Snackbar.LENGTH_INDEFINITE)
                .setAction("晓得"){
                    it.isVisible = false
                }
                .show()

        }

//        fun updateRecyclerView(){
//            val packs=packageManager.getInstalledPackages(0)
//            val userAppList:ArrayList<PackageInfo> = ArrayList()
//            val systemAppList:ArrayList<PackageInfo> =ArrayList()
//
//            for ( pkginfo in packs) {
//                pkginfo.let {
//                    if (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM <= 0)
//                        userAppList.add(it)
//                    else
//                        systemAppList.add(it)
//                }
//                itemView.recyclerView.adapter = MyRecyclerViewAdapter(systemAppList)
//                itemView.recyclerView.layoutManager= LinearLayoutManager(context)
//            }
//
//        }

    }
}