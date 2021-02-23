package me.heizi.box.package_manager.dao

import androidx.room.Database
import androidx.room.RoomDatabase
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
abstract class RoomDB: RoomDatabase() {
//        inline fun migration(start:Int,end:Int,crossinline block: (SupportSQLiteDatabase) -> Unit) = object : Migration(1,2) { override fun migrate(database: SupportSQLiteDatabase) = block(database) }

    abstract fun getDefaultManager():DBManager
}