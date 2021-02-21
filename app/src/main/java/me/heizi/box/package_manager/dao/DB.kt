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
//        inline fun migration(start:Int,end:Int,crossinline block: (SupportSQLiteDatabase) -> Unit) = object : Migration(1,2) { override fun migrate(database: SupportSQLiteDatabase) = block(database) }
        private var instance:DB? = null
        val INSTANCE get() = instance!!
        fun resign(context: Context){
            instance =Room.databaseBuilder(context,DB::class.java,"uninstall_data")
                .build()
        }
        fun <T> CoroutineScope.database(
            dispatcher: CoroutineDispatcher = IO
            ,block:suspend DB.()->T
        ) = async(dispatcher) { block(INSTANCE) }
        fun <T> CoroutineScope.databaseMapper(
            dispatcher: CoroutineDispatcher = IO
            ,block:suspend DBMapper.()->T
        ) = async(dispatcher) { block(INSTANCE.getDefaultMapper()) }

        fun CoroutineScope.updateDB(
                dispatcher: CoroutineDispatcher = IO,
                block: DB.()->Unit
        ) = launch(dispatcher) { block(INSTANCE) }
    }
    abstract fun getDefaultMapper():DBMapper


    fun Connect.add() = getDefaultMapper().add(this)
    fun Version.add() = getDefaultMapper().add(this)
    fun UninstallRecord.add() = getDefaultMapper().add(this)
    fun Connect.delete() = getDefaultMapper().delete(this)
    fun Version.delete() = getDefaultMapper().delete(this)
    fun UninstallRecord.delete() = getDefaultMapper().delete(this)
    fun Connect.update() = getDefaultMapper().update(this)
    fun Version.update() = getDefaultMapper().update(this)
    fun UninstallRecord.update() = getDefaultMapper().update(this)
}
