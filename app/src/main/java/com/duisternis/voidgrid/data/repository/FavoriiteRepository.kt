package com.duisternis.voidgrid.data.repository

import android.content.Context
import coil.ImageLoader
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

    suspend fun updateFolder(folder: FolderEntity) =
        dao.updateFolder(folder)

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
     * Salva um pin. O sinal preferencial para o For You é [sourceQuery]
     * (intenção explícita do usuário) + categorias da pasta.
     * ML Kit removido — sem fallback de visão computacional.
     */
    suspend fun pinItem(item: SearchItem, folderId: Long, sourceQuery: String? = null) {
        val dominantColor = ColorCategorizer.categorizeFromUrl(
            thumbnailUrl = item.thumbnail,
            imageLoader = imageLoader,
            context = appContext
        )

        dao.insertPin(
            PinEntity(
                folderId = folderId,
                link = item.link,
                thumbnail = item.thumbnail,
                source = item.source,
                width = item.width,
                height = item.height,
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