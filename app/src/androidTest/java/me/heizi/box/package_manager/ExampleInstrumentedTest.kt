package me.heizi.box.package_manager

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

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

//    @Test
//    fun compressor() {
//        val apps = LinkedList<UninstallRecord>()
//        repeat(50) {
//            UninstallRecord(name = "name$it",packageName = "me.heizi.p$it",source = "/path/$it",data = null,id= it)
//                .let(apps::add)
//        }
//        val job = GlobalScope.launch {
//            Compressor.generateV1(VersionConnected(
//                name = "123",
//                apps = apps,
//                createTime = System.currentTimeMillis().toInt(),
//            )).let {
//                Log.i(TAG, "compressor: $it")
//                val json = Compressor.buildJson(it)
//                Log.i(TAG, "compressor: $json")
//            }
//        }
//        while (job.isActive) {
//            Thread.sleep(10)
//        }
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