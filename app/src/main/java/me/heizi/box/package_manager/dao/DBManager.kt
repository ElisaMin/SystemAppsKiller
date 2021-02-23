package me.heizi.box.package_manager.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version


@Dao
interface DBManager {

    @Query("select * from connecting")
    fun allConnect():Flow<List<Connect>>
    @Query("select * from recording")
    fun allUninstall():Flow<List<UninstallRecord>>
    @Query("select * from versions")
    fun allVersion(): Flow<List<Version>>
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