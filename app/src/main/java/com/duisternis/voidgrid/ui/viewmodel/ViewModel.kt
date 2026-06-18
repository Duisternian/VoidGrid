package com.duisternis.voidgrid.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

// ─── ViewModel de busca de imagens ───────────────────────────────────────────

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

    // Cache de cores — sobrevive a recomposições
    private val _colorCache = mutableMapOf<String, Color>()
    val colorCache: Map<String, Color> get() = _colorCache

    fun cacheColor(thumbnailUrl: String, color: Color) {
        _colorCache[thumbnailUrl] = color
    }

    // loadedKeys e errorKeys no ViewModel — evita recomposição do grid inteiro
    // usando Set<String> com mutableStateOf para notificar só quem lê cada chave
    private val _loadedKeys = mutableSetOf<String>()
    private val _errorKeys = mutableSetOf<String>()

    // Estado observável para o Compose saber quando atualizar
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
        loadedKeysVersion = 0
        errorKeysVersion = 0
        _query.value = newQuery
    }
}