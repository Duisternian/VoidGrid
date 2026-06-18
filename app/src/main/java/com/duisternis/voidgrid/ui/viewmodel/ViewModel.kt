package com.duisternis.voidgrid.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class ImageSearchViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query
        .filterNotNull()
        .filter { it.isNotBlank() }
        .flatMapLatest { repository.searchImages(it) }
        .cachedIn(viewModelScope)

    // Cache de cores reativo — mutableStateMapOf notifica o Compose quando atualiza
    private val _colorCache = androidx.compose.runtime.mutableStateMapOf<String, Color>()
    val colorCache: Map<String, Color> get() = _colorCache

    fun cacheColor(thumbnailUrl: String, color: Color) {
        _colorCache[thumbnailUrl] = color
    }

    // Cache de sugestões fixas por link:
    // - filtra o item original E deduplica por link antes de shufflar
    // - garante que fechar/reabrir a dialog mostre sempre as mesmas sugestões
    private val _suggestionsCache = mutableMapOf<String, List<SearchItem>>()

    fun getSuggestions(link: String, allImages: List<SearchItem>): List<SearchItem> {
        return _suggestionsCache.getOrPut(link) {
            allImages
                .distinctBy { it.link }   // remove duplicatas da lista da API
                .filter { it.link != link } // remove o próprio item aberto
                .shuffled()
                .take(20)
        }
    }

    // loadedKeys e errorKeys no ViewModel
    private val _loadedKeys = mutableSetOf<String>()
    private val _errorKeys = mutableSetOf<String>()

    var loadedKeysVersion by mutableStateOf(0)
        private set
    var errorKeysVersion by mutableStateOf(0)
        private set

    fun markLoaded(link: String) {
        if (_loadedKeys.add(link)) loadedKeysVersion++
    }

    fun markError(link: String) {
        if (_errorKeys.add(link)) errorKeysVersion++
    }

    fun isLoaded(link: String) = link in _loadedKeys
    fun isError(link: String) = link in _errorKeys

    fun search(newQuery: String) {
        if (newQuery.isBlank() || newQuery == _query.value) return
        _colorCache.clear()
        _loadedKeys.clear()
        _errorKeys.clear()
        _suggestionsCache.clear()
        loadedKeysVersion = 0
        errorKeysVersion = 0
        _query.value = newQuery
    }
}