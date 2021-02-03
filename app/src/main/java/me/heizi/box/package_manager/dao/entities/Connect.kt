package me.heizi.box.package_manager.dao.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "connecting",
    foreignKeys = [
        ForeignKey(
                entity = UninstallRecord::class,
                parentColumns = ["id"],
                childColumns = ["record_id"]
        ),
        ForeignKey(
                entity = Version::class,
                parentColumns = ["id"],
                childColumns = ["version_id"],
                onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Connect(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "version_id",index = true)
    val version:Int=0,
    @ColumnInfo(name = "record_id",index = true)
    val record:Int =0
)