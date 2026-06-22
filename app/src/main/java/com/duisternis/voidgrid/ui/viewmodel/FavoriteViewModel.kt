package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: FavoritesRepository
) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPins: StateFlow<List<PinEntity>> = repository.allPins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun isPinned(link: String): Flow<Boolean> = repository.isPinned(link)

    fun getCoverPin(folderId: Long): Flow<PinEntity?> = repository.getCoverPin(folderId)

    fun getPinsForFolder(folderId: Long): Flow<List<PinEntity>> =
        repository.getPinsForFolder(folderId)

    fun getPinsAsSearchItems(folderId: Long): Flow<List<SearchItem>> =
        repository.getPinsForFolder(folderId).map { pins ->
            pins.map { with(repository) { it.toSearchItem() } }
        }

    fun getAllPinsAsSearchItems(): Flow<List<SearchItem>> =
        repository.allPins.map { pins ->
            pins.map { with(repository) { it.toSearchItem() } }
        }

    /**
     * @param sourceQuery a busca ativa na tela no momento do save (baseQuery
     * do ImageDetailDialog). Veja FavoritesRepository.pinItem para detalhes.
     */
    fun pinItem(item: SearchItem, folderId: Long, sourceQuery: String? = null) {
        viewModelScope.launch { repository.pinItem(item, folderId, sourceQuery) }
    }

    fun unpinItem(link: String) {
        viewModelScope.launch { repository.unpinByLink(link) }
    }

    fun createFolder(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createFolder(name)
            onCreated(id)
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }
}