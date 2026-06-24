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
    val savedAt: Long = System.currentTimeMillis(),
    // A busca ativa quando o usuário salvou este pin.
    // Ex: "mork borg", "matrix", "kult divinity lost"
    // Combinado com folder.categories forma a query do For You.
    val sourceQuery: String? = null,
    // Cor dominante da thumbnail para filtro de cor na API do DuckDuckGo
    // (Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink, Brown, Black, White, Gray)
    val dominantColor: String? = null
)