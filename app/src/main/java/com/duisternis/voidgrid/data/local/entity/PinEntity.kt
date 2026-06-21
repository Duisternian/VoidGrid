package com.duisternis.voidgrid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pins",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class PinEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,
    val link: String,
    val thumbnail: String?,
    val source: String,
    val width: Int = 0,
    val height: Int = 0,
    val savedAt: Long = System.currentTimeMillis()
)