package me.heizi.box.package_manager.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version

@Database(
    entities = [
        Connect::class,
        UninstallRecord::class,
        Version::class
    ],
    exportSchema = false,
    version = 1
)
abstract class DB: RoomDatabase() {
    companion object {
        private var instance:DB? = null
        val INSTANCE get() = instance!!
        fun resign(context: Context){
            instance =Room.databaseBuilder(context,DB::class.java,"uninstall_data").build()
        }
        fun <T> CoroutineScope.database(
            dispatcher: CoroutineDispatcher = IO
            ,block:DB.()->T
        ) = async(dispatcher) { block(INSTANCE) }
        fun CoroutineScope.updateDB(
                dispatcher: CoroutineDispatcher = IO,
                block:DB.()->Unit
        ) = launch(dispatcher) { block(INSTANCE) }
    }
    abstract fun getMapper():DBMapper


    fun Connect.add() = getMapper().add(this)
    fun Version.add() = getMapper().add(this)
    fun UninstallRecord.add() = getMapper().add(this)
    fun Connect.delete() = getMapper().delete(this)
    fun Version.delete() = getMapper().delete(this)
    fun UninstallRecord.delete() = getMapper().delete(this)
    fun Connect.update() = getMapper().update(this)
    fun Version.update() = getMapper().update(this)
    fun UninstallRecord.update() = getMapper().update(this)
}
