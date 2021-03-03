package me.heizi.box.package_manager.log

import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Entity
data class LogSaver(
    @PrimaryKey(autoGenerate = true)
    val id:Int? = null,
    val message:String?,
    val process:String?,
    val level:String
)
@Dao
interface LogManager {
    @Insert
    fun log(log: LogSaver)
    fun save(message: String?, process:String?, level:String) { GlobalScope.launch(Dispatchers.Unconfined) { log(LogSaver(message = message,process = process,level = level)) } }
}
@Database(exportSchema = false,entities = [LogSaver::class],version = 1,)
abstract class LogHouse:RoomDatabase() {
    val manager get() = getLogManager()
    abstract fun getLogManager():LogManager
}
