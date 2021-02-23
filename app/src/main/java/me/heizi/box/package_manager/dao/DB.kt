package me.heizi.box.package_manager.dao

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import me.heizi.box.package_manager.Application.Companion.TAG
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object DB:LifecycleObserver {

//    @ExperimentalStdlibApi
//    @JvmStatic
//    fun main(args: Array<String>) {
//        for (i in arrayOf("Uninstalleds","Versions","Connections")) {
//            println("""_${i.lowercase()} = null""")
//            println("""_as$i = null""")
//        }
//    }
    val asUninstalleds get() = _asUninstalleds!!
    val asConnections get() = _asConnections!!
    val asVersions get() = _asVersions!!

    val uninstalleds get() = _uninstalleds!!
    val connections get() = _connections!!
    val versions get() = _versions!!

    private val scope get() = _scope!!
    private val manager get() = _manager!!
    private lateinit var handler:CoroutineDispatcher
    private var pool:ExecutorService? = null
    private var _scope:CoroutineScope? = null
    private var _manager: DBManager? = null
    private var _uninstalleds:List<UninstallRecord>? = null
    private var _connections:List<Connect>? = null
    private var _versions:List<Version>? = null
    private var instance:RoomDB? = null
    private var _asUninstalleds: Flow<List<UninstallRecord>>? = null
    private var _asConnections: Flow<List<Connect>>?=null
    private var _asVersions: Flow<List<Version>>? = null
    private fun start(block: suspend CoroutineScope.()->Unit) = scope.launch(block = block)
    private fun setFlowNotNull() {
        if (_asUninstalleds == null) _asUninstalleds = manager.allUninstall()
        if (_asConnections == null) _asConnections = manager.allConnect()
        if (_asVersions == null) _asVersions = manager.allVersion()
    }
    private fun releaseRam() {
        _uninstalleds = null
        _asUninstalleds = null
        _versions = null
        _asVersions = null
        _connections = null
        _asConnections = null

    }
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        pool = Executors.newFixedThreadPool(4)
        handler = pool!!.asCoroutineDispatcher()
        _scope = CoroutineScope(handler)
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume():Unit {
        try {
            setFlowNotNull()
            start { asUninstalleds.collect {
                _uninstalleds = it
            } };start { asConnections.collect {
                _connections = it
            } };start { asVersions.collect {
                _versions = it
            } }
        }catch (e:Exception) {
            Log.e(TAG, "onResume: caught error", e)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        scope.cancel()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() = try{
        pool!!.shutdown()
        pool = null
        _scope = null
        _manager = null
    }catch (e:Exception) { }

    fun resign(context: Context){
        instance =Room.databaseBuilder(context,RoomDB::class.java,"uninstall_data").build()
        _manager = instance!!.getDefaultManager()
    }



    operator fun minus(other:UninstallRecord) { manager.delete(other)}
    operator fun minus(other:Version) { manager.delete(other)}
    operator fun minus(other:Connect) { manager.delete(other)}
    operator fun plus(other:UninstallRecord) { manager.add(other)}
    operator fun plus(other:Version) { manager.add(other)}
    operator fun plus(other:Connect) { manager.add(other)}
    infix fun UPDATE(other:UninstallRecord) { manager.update(other)}
    infix fun UPDATE(other:Version) { manager.update(other)}
    infix fun UPDATE(other:Connect) { manager.update(other)}
}