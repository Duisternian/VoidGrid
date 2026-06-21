package com.duisternis.voidgrid.data.local.dao

import androidx.room.*
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinsDao {

    // ─── Pastas ───────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    // ─── Pins ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: PinEntity)

    @Delete
    suspend fun deletePin(pin: PinEntity)

    @Query("SELECT * FROM pins WHERE folderId = :folderId ORDER BY savedAt DESC")
    fun getPinsForFolder(folderId: Long): Flow<List<PinEntity>>

    @Query("SELECT * FROM pins ORDER BY savedAt DESC")
    fun getAllPins(): Flow<List<PinEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM pins WHERE link = :link)")
    fun isPinned(link: String): Flow<Boolean>

    @Query("DELETE FROM pins WHERE link = :link")
    suspend fun deletePinByLink(link: String)

    // Capa da pasta — pega o pin mais recente
    @Query("SELECT * FROM pins WHERE folderId = :folderId ORDER BY savedAt DESC LIMIT 1")
    fun getCoverPin(folderId: Long): Flow<PinEntity?>
}