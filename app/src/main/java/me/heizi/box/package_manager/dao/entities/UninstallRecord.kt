package me.heizi.box.package_manager.dao.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.heizi.box.package_manager.models.UninstallInfo


@Entity(tableName = "recording")
data class UninstallRecord (
    @PrimaryKey @ColumnInfo(index = true)
    val id: Int? = null,
    val name:String,
    override val packageName:String,
    val source:String,
    val data:String? = null,
    @ColumnInfo(name = "is_backup")
    val isBackups: Boolean = false
): UninstallInfo {
    override val applicationName: String get() = name
    override val sourceDirectory: String get() = source
}

