package com.duisternis.voidgrid.data.repository

import com.duisternis.voidgrid.data.local.dao.PinsDao
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val dao: PinsDao) {

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

    suspend fun pinItem(item: SearchItem, folderId: Long) {
        dao.insertPin(
            PinEntity(
                folderId = folderId,
                link = item.link,
                thumbnail = item.thumbnail,
                source = item.source,
                width = item.width,
                height = item.height
            )
        )
    }

    suspend fun unpinByLink(link: String) =
        dao.deletePinByLink(link)

    // Converte PinEntity de volta para SearchItem para reutilizar
    // os composables existentes (DominantColorBox, AsyncImage, etc.)
    fun PinEntity.toSearchItem() = SearchItem(
        link = link,
        source = source,
        width = width,
        height = height,
        thumbnail = thumbnail
    )
}