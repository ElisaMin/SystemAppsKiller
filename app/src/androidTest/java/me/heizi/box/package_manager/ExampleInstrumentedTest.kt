package me.heizi.box.package_manager

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
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
    @Test
    fun useAppContext() {
        // Context of the app under test.


//        assertEquals("me.heizi.box.package_manager", appContext.packageName)
    }

    @Test
    fun packageManagerTest() {
//        println("is println only can println to log?")
        isSystemOnlyWork()
        installedAppSize()
        isAndWorkingWill()
        getSystemApps()
//        whatAboutUserApps()
    }
    val withApk = """[/\w+]+/.+\.apk""".toRegex()
    val hasNoApk = """[/\w+]+""".toRegex()
    val appList:HashMap<String,ArrayList<PackageInfo>> = hashMapOf()
    fun getPreviousPath(path:String):String {
        fun notNormalPath(): Nothing = throw IllegalArgumentException("$path 非正常path")
        val list = if (path.isEmpty() || !path.contains("/")) {
            notNormalPath()
        } else path.split("/",ignoreCase = true).toMutableList()
        when {
            path.matches(withApk) -> {
                list.removeLast()
                list.removeLast()
            }
            path.matches(hasNoApk) -> {
                list.removeLast()
            }
            else -> notNormalPath()
        }
        return StringBuilder().apply {
            list.forEach {
                append(it)
                append("/")
            }
        }.toString().dropLast(1)
    }
    @Test
    fun getPaths() {

        val time = System.currentTimeMillis()
        getAllSystemApps().forEach {i->
            getPreviousPath(i.applicationInfo.sourceDir).let { p->
                appList[p]?.add(i) ?: kotlin.run { appList[p] = arrayListOf(i) }
            }
        }
        appList.forEach { (k, v) ->
            v.forEach {
                Log.i(TAG,"$k:{path:${it.applicationInfo.sourceDir},name:${it.packageName}}")
            }
        }
        Log.i(TAG, "getPaths: ${System.currentTimeMillis()-time}")
    }

    fun getAllSystemApps() = appContext.packageManager.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY)

    private fun getSystemApps() { //网上面抄的
        var system = 0
        var user = 0
        var all = 0
        for ( p in appContext.packageManager.getInstalledPackages(0)) {
            if (p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM <= 0) user ++
            else system ++
            all++
        }
        Log.i(TAG, "getSystemApps: system:$system,user:$user,all:$all")
    }

    fun isSystemOnlyWork() { // working fine
        Log.i(TAG, "isSystemOnlyWork: ${
            appContext.packageManager.getInstalledPackages(PackageManager.MATCH_SYSTEM_ONLY).size
        }")
    }
    fun whatAboutUserApps() { // working fine
        Log.i(TAG, "whatAboutUserApp: ${
            appContext.packageManager.getInstalledPackages( PackageManager.MATCH_SYSTEM_ONLY.inv()).size
        }")
    }
    fun installedAppSize() { //same result
        Log.i(TAG, "installedAppSize: ${
            appContext.packageManager.getInstalledPackages(PackageManager.MATCH_ALL).size    
        }")
    }
    fun isAndWorkingWill() {// && didn't work
        Log.i(TAG, "isAndWorkingWill: ${
            appContext.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES xor PackageManager.MATCH_SYSTEM_ONLY ).size
        },")
    }
    @Test
    fun packageManagerFlags() {
        println(PackageManager.GET_ACTIVITIES)
        println(PackageManager.MATCH_SYSTEM_ONLY)
        println(PackageManager.MATCH_SYSTEM_ONLY.inv())
        println()
        println(PackageManager.MATCH_SYSTEM_ONLY xor PackageManager.GET_ACTIVITIES )
    }
}