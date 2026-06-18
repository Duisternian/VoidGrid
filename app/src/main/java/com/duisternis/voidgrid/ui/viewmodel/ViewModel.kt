package com.duisternis.voidgrid.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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

    // Cache de cores reativo — notifica o Compose automaticamente quando atualiza
    private val _colorCache = mutableStateMapOf<String, Color>()
    val colorCache: Map<String, Color> get() = _colorCache

    fun cacheColor(thumbnailUrl: String, color: Color) {
        _colorCache[thumbnailUrl] = color
    }

    // Links de imagens com transparência detectada no onSuccess — sem gradiente
    private val _transparentKeys = mutableStateMapOf<String, Boolean>()
    val transparentKeys: Map<String, Boolean> get() = _transparentKeys

    fun markTransparent(link: String) {
        _transparentKeys[link] = true
    }

    fun isTransparent(link: String): Boolean = _transparentKeys[link] == true

    // Cache de sugestões fixas por link
    private val _suggestionsCache = mutableMapOf<String, List<SearchItem>>()

    fun getSuggestions(link: String, allImages: List<SearchItem>): List<SearchItem> {
        return _suggestionsCache.getOrPut(link) {
            allImages
                .distinctBy { it.link }
                .filter { it.link != link }
                .shuffled()
                .take(20)
        }
    }

    // Sets normais + versão reativa para notificar o Compose
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

    fun isLoaded(link: String): Boolean {
        @Suppress("UNUSED_EXPRESSION") loadedKeysVersion
        return link in _loadedKeys
    }

    fun isError(link: String): Boolean {
        @Suppress("UNUSED_EXPRESSION") errorKeysVersion
        return link in _errorKeys
    }

    fun search(newQuery: String) {
        if (newQuery.isBlank() || newQuery == _query.value) return
        _colorCache.clear()
        _loadedKeys.clear()
        _errorKeys.clear()
        _transparentKeys.clear()
        _suggestionsCache.clear()
        loadedKeysVersion = 0
        errorKeysVersion = 0
        _query.value = newQuery
    }
}