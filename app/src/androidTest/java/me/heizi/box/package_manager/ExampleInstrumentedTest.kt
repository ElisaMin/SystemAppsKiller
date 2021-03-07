package me.heizi.box.package_manager

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.models.VersionConnected
import org.junit.runner.RunWith
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
private const val TAG = "ExampleInstrumentedTest"
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val appContext by lazy { InstrumentationRegistry.getInstrumentation().targetContext }

//    fun emit(ondone:()->Unit) {
//        MainScope().launch(Dispatchers.Main) {
//            repeat(100) {
//                Random(it).nextInt(10).let { r->
//                    Log.i(Application.TAG, "emit: $r")
//                    (if (r>5) CommandResult.Success("")
//                    else CommandResult.Failed("use less","its a error message",3))
//                            .let {s->
//                                collectResults(UninstallInfo("anyway$it","ane","gadg","dgdfd",) to s)
//                            }
//                    delay(300)
//                }
//            }
//            stopSelf()
////                ondone()
//        }
//    }
    private fun printlns(any: Any?) {
        Log.d(TAG, "println: {${any.toString()}}")
    }
    private fun println(any: Any?) {
        Log.d(TAG, "println: {${any.toString()}}")
    }
//    fun createNewVersion(name:String,time:Int,list:List<UninstallRecord>) {
//        val m = DB.INSTANCE.MAPPER
//        //把版本插入数据库
//
//        //获得该版本的id
//        //把List放进来
//    }
//    @Test
//    fun db() {
//        Log.d(TAG, "db: failese")
//        DB.resign(appContext)
//        runBlocking {
//            DB.INSTANCE.MAPPER.getAllConnected().collect {
//                println(it)
//                cancel()
//            }
//        }
//    }

    private val apps get() = LinkedList<UninstallRecord>().apply {
        appContext.packageManager.getInstalledPackages(0).forEachIndexed { index, packageInfo ->
            if (index<60) add(UninstallRecord(id = index,name = appContext.packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),packageName = packageInfo.packageName,source = packageInfo.applicationInfo.sourceDir,isBackups = false))
        }
    }
    private val connected get() = VersionConnected(
        name = "123",
        apps = apps,
        createTime = System.currentTimeMillis().toInt(),
    )

//    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            println((1).toByte())
//        }
//    }
//    @Test
//    fun qc_code() {
//        println("start")
//        val connected = connected
//        println(connected.apps.size)
//        CompressorVersion.V1.encodeForImage(connected).size.let(::println)
//        println("connected")
//        connected.toQrCode()!!.let {
//            println("not null")
//            Compressor.read(it)
//        }.let {
//            var listIsSame = apps.size == connected.apps.size
//            if (listIsSame) for ( i in 0 until apps.size) {
//                listIsSame = apps[i].packageName==connected.apps[i].packageName
//                if(!listIsSame) break
//            }
//            listIsSame
//        }.let(::println)
//    }
//    @Test
//    fun compressorNew() {
//        VersionConnected(
//            name = "123",
//            apps = apps,
//            createTime = System.currentTimeMillis().toInt(),
//        ).also {
//            it.toQrCode()
//        }.toShareableText()
//            .let { println(it) }
//    }
//    @Test
//    fun compressor() {
//        val split = arrayOf("/"," ","-",".","_")
//        val apps = LinkedList<UninstallRecord>()
//        appContext.packageManager.getInstalledPackages(0).forEachIndexed { index, packageInfo ->
//            apps.add(UninstallRecord(id = index,name = appContext.packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(),packageName = packageInfo.packageName,source = packageInfo.applicationInfo.sourceDir,isBackups = false))
//        }
//        val time = System.currentTimeMillis()
//        var times = 0
//        val splited = HashMap<String,Int>()
//        fun check(list:List<String>) {
//            list.forEach {
//                splited[it] = splited[it]?.plus(1) ?:1
//                times++
//            }
//        }
//        fun split(string: String) {
//            check(string.split(*split,ignoreCase = true))
//        }
//        apps.forEach {
//            it.source.let(::split)
//            it.packageName.let(::split)
//            it.applicationName.let(::split)
//        }
//        val byteLenght = (splited.size/256)+1
//        val byteArray = ByteArrayOutputStream()
//
//        splited.forEach { t, u -> println("word:$t,times:$u") }
//
////        val sb = StringBuilder()
////        sb.append("{v:2,d:'")
////        sb.append("'}")
////        val result = sb.toString().replace("}{","},{")
////        println(System.currentTimeMillis() - time)
//        println(times)
////        runBlocking {
////            Compressor.generateV1(VersionConnected(
////                name = "123",
////                apps = apps,
////                createTime = System.currentTimeMillis().toInt(),
////            )).let {
////                File(appContext.cacheDir,"uninstall-${System.currentTimeMillis().toString().takeLast(6)}.json").let {
////                    it.bufferedWriter()
////                }.let { b->
////                    b.append(it)
////                    b.flush()
////                    b.close()
////                }
////
////                println(it.length)
////                for (i in it.split("/")) println(i)
//////                Log.i(TAG, "compressor: $it")
//////                val json = Compressor.buildJson(it)
//////                Log.i(TAG, "compressor: $json")
////            }
////        }
//    }

//    @Test
//    fun service() {
//        var running = true
//        var binder:CleaningAndroidService.Binder? = null
//        val intent = Intent(appContext,CleaningAndroidService::class.java)
//        intent.action = Intent.ACTION_USER_FOREGROUND
//        val connect = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                binder = (service as CleaningAndroidService.Binder)
//                Log.i(TAG, "onServiceConnected: called")
////                binder!!.emit {
////                    Log.i(TAG, "onServiceConnected: stoping")
////                    appContext.stopService(intent)
////                    running = false
////                }
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//
//            }
//        }
//                intent
//                .also(appContext::startService)
//                .also { appContext.bindService(it,connect, Context.BIND_AUTO_CREATE) }
//        while (running) {
//
//        }
//    }
//
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//
//
////        assertEquals("me.heizi.box.package_manager", appContext.packageName)
//    }
//
//    @Test
//    fun packageManagerTest() {
////        println("is println only can println to log?")
//        isSystemOnlyWork()
//        installedAppSize()
//        isAndWorkingWill()
//        getSystemApps()
////        whatAboutUserApps()
//    }
//    val withApk = """[/\w+]+/.+\.apk""".toRegex()
//    val hasNoApk = """[/\w+]+""".toRegex()
//    val appList: HashMap<String, ArrayList<PackageInfo>> = hashMapOf()
//    fun getPreviousPath(path:String):String {
//        fun notNormalPath(): Nothing = throw IllegalArgumentException("$path 非正常path")
//        val list = if (path.isEmpty() || !path.contains("/")) {
//            notNormalPath()
//        } else path.split("/",ignoreCase = true).toMutableList()
//        when {
//            path.matches(withApk) -> {
//                list.removeLast()
//                list.removeLast()
//            }
//            path.matches(hasNoApk) -> {
//                list.removeLast()
//            }
//            else -> notNormalPath()
//        }
//        return StringBuilder().apply {
//            list.forEach {
//                append(it)
//                append("/")
//            }
//        }.toString().dropLast(1)
//    }
//    @Test
//    fun getPaths() {
//
//        val time = System.currentTimeMillis()
//        getAllSystemApps().forEach {i->
//            getPreviousPath(i.applicationInfo.sourceDir).let { p->
//                appList[p]?.add(i) ?: kotlin.run { appList[p] = arrayListOf(i) }
//            }
//        }
//        appList.forEach { (k, v) ->
//            v.forEach {
//                Log.i(TAG,"$k:{path:${it.applicationInfo.sourceDir},name:${it.packageName}}")
//            }
//        }
//        Log.i(TAG, "getPaths: ${System.currentTimeMillis()-time}")
//    }
//
//    fun getAllSystemApps() = appContext.packageManager.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY)
//
//    private fun getSystemApps() { //网上面抄的
//        var system = 0
//        var user = 0
//        var all = 0
//        for ( p in appContext.packageManager.getInstalledPackages(0)) {
//            if (p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM <= 0) user ++
//            else system ++
//            all++
//        }
//        Log.i(TAG, "getSystemApps: system:$system,user:$user,all:$all")
//    }
//
//    fun isSystemOnlyWork() { // working fine
//        Log.i(TAG, "isSystemOnlyWork: ${
//            appContext.packageManager.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY).size
//        }")
//    }
//    fun whatAboutUserApps() { // working fine
//        Log.i(TAG, "whatAboutUserApp: ${
//            appContext.packageManager.getInstalledPackages( PackageManager.MATCH_SYSTEM_ONLY.inv()).size
//        }")
//    }
//    fun installedAppSize() { //same result
//        Log.i(TAG, "installedAppSize: ${
//            appContext.packageManager.getInstalledPackages(PackageManager.MATCH_ALL).size
//        }")
//    }
//    fun isAndWorkingWill() {// && didn't work
//        Log.i(TAG, "isAndWorkingWill: ${
//            appContext.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES xor PackageManager.MATCH_SYSTEM_ONLY ).size
//        },")
//    }
//    @Test
//    fun packageManagerFlags() {
//        println(PackageManager.GET_ACTIVITIES)
//        println(PackageManager.MATCH_SYSTEM_ONLY)
//        println(PackageManager.MATCH_SYSTEM_ONLY.inv())
//        println()
//        println(PackageManager.MATCH_SYSTEM_ONLY xor PackageManager.GET_ACTIVITIES )
//    }
}