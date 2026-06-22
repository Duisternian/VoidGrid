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
    // Tags do ML Kit — agora usadas só como FALLBACK quando sourceQuery é null
    // (ex: pin sem busca textual associada, como import futuro de board externo)
    val tags: String = "",
    // A busca que estava ativa quando o usuário salvou este pin.
    // Ex: "Blame! mangá", "matrix", "mork borg poster"
    // Fonte de sinal MAIS confiável — vem da intenção explícita do usuário,
    // não de inferência visual.
    val sourceQuery: String? = null,
    // Cor dominante da thumbnail, já normalizada para uma das categorias
    // aceitas pelo filtro de cor da API interna do DuckDuckGo
    // (Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink, Brown, Black, White, Gray).
    val dominantColor: String? = null
)