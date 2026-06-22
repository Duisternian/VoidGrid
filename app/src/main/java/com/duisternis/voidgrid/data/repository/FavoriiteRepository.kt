package com.duisternis.voidgrid.data.repository

import android.content.Context
import coil.ImageLoader
import com.duisternis.voidgrid.data.local.ImageLabeler
import com.duisternis.voidgrid.data.local.dao.PinsDao
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.util.ColorCategorizer
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(
    private val dao: PinsDao,
    private val imageLoader: ImageLoader,
    private val appContext: Context
) {

    // ─── Pastas ───────────────────────────────────────────────────────────────

    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()

    suspend fun createFolder(name: String): Long =
        dao.insertFolder(FolderEntity(name = name))

    suspend fun deleteFolder(folder: FolderEntity) =
        dao.deleteFolder(folder)

    fun getCoverPin(folderId: Long): Flow<PinEntity?> =
        dao.getCoverPin(folderId)

    // ─── Pins ─────────────────────────────────────────────────────────────────

    val allPins: Flow<List<PinEntity>> = dao.getAllPins()

    fun getPinsForFolder(folderId: Long): Flow<List<PinEntity>> =
        dao.getPinsForFolder(folderId)

    fun isPinned(link: String): Flow<Boolean> =
        dao.isPinned(link)

    /**
     * @param sourceQuery a busca que estava ativa quando o usuário salvou este
     * pin (ex: "Blame! mangá"). Esta é a fonte de sinal preferencial para o
     * feed "Para Você" — mais confiável que tags geradas por visão computacional,
     * porque reflete a intenção explícita do usuário. Quando null/blank, o
     * ML Kit roda como fallback para gerar tags genéricas.
     */
    suspend fun pinItem(item: SearchItem, folderId: Long, sourceQuery: String? = null) {
        // Cor dominante — sempre tentamos extrair, independente de ter sourceQuery,
        // pois ela complementa tanto a busca textual quanto o fallback do ML Kit.
        val dominantColor = ColorCategorizer.categorizeFromUrl(
            thumbnailUrl = item.thumbnail,
            imageLoader = imageLoader,
            context = appContext
        )

        // ML Kit só roda como fallback — economiza processamento quando já
        // temos um sinal confiável (sourceQuery) vindo da busca do usuário.
        val tags = if (sourceQuery.isNullOrBlank()) {
            item.thumbnail
                ?.let { ImageLabeler.labelsFromUrl(it) }
                ?.joinToString(",")
                ?: ""
        } else {
            ""
        }

        dao.insertPin(
            PinEntity(
                folderId = folderId,
                link = item.link,
                thumbnail = item.thumbnail,
                source = item.source,
                width = item.width,
                height = item.height,
                tags = tags,
                sourceQuery = sourceQuery?.trim()?.takeIf { it.isNotBlank() },
                dominantColor = dominantColor
            )
        )
    }

    suspend fun unpinByLink(link: String) =
        dao.deletePinByLink(link)

    fun PinEntity.toSearchItem() = SearchItem(
        link = link,
        source = source,
        width = width,
        height = height,
        thumbnail = thumbnail
    )
}