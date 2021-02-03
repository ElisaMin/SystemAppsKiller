package me.heizi.box.package_manager.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version

@Dao
interface DBMapper {





    @Insert
    fun add(version: Version)
    @Insert
    fun add(uninstallRecord: UninstallRecord)
    @Insert
    fun add(connect: Connect)
    @Delete
    fun delete(version: Version)
    @Delete
    fun delete(uninstallRecord: UninstallRecord)
    @Delete
    fun delete(connect: Connect)
    @Update
    fun update(version: Version)
    @Update
    fun update(uninstallRecord: UninstallRecord)
    @Update
    fun update(connect: Connect)
}