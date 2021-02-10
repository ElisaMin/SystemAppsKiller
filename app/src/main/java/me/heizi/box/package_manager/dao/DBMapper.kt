package me.heizi.box.package_manager.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.heizi.box.package_manager.dao.entities.Connect
import me.heizi.box.package_manager.dao.entities.UninstallRecord
import me.heizi.box.package_manager.dao.entities.Version

@Dao
interface DBMapper {





    @Query("select * from versions")
    fun getALlVersions(): Flow<List<Version>>
    @Query("select * from connecting")
    fun getAllConnected(): Flow<List<Connect>>
    @Query("select * from recording")
    fun getAllUninstalled(): Flow<List<UninstallRecord>>
    @Query("select max(id) from versions")
    fun getLastVersion():Int?
    @Query("select * from versions where name = :name and create_time = :time")
    fun findVersion(name:String,time:Int):Version?
    @Query("select count(*) from recording")
    fun getUninstalledCount():Int
    @Query("select r.* from  recording r,connecting c where r.id = c.record_id and version_id =:versionId ")
    fun findVersionUninstallList(versionId:Int):List<UninstallRecord>

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