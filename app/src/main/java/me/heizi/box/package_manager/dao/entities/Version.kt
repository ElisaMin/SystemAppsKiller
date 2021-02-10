package me.heizi.box.package_manager.dao.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "versions")
data class Version(
    @PrimaryKey @ColumnInfo(index = true)
    val id: Int? = null,
    val name:String,
    @ColumnInfo(name = "create_time")
    val createTime:Int
)