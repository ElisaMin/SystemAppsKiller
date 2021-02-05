package me.heizi.box.package_manager.dao.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "recording")
data class UninstallRecord(
    @PrimaryKey @ColumnInfo(index = true)
    val id: Int? = null,
    val name:String,
    val packageName:String,
    val source:String,
    val data:String?,
    @ColumnInfo(name = "is_backup")
    val isBackups: Boolean = false
)

