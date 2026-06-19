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
import kotlinx.coroutines.launch

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

    private val _colorCache = mutableStateMapOf<String, Color>()
    val colorCache: Map<String, Color> get() = _colorCache

    fun cacheColor(thumbnailUrl: String, color: Color) {
        _colorCache[thumbnailUrl] = color
    }

    private val _transparentKeys = mutableStateMapOf<String, Boolean>()
    val transparentKeys: Map<String, Boolean> get() = _transparentKeys

    fun markTransparent(link: String) {
        _transparentKeys[link] = true
    }

    fun isTransparent(link: String): Boolean = _transparentKeys[link] == true

    // Cache de sugestões refinadas por link — evita refazer a busca por domínio
    // toda vez que o usuário troca de imagem dentro do mesmo Dialog
    private val _refinedSuggestionsCache = mutableMapOf<String, List<SearchItem>>()

    // Estado reativo do loading das sugestões refinadas, por link
    private val _suggestionsLoading = mutableStateMapOf<String, Boolean>()
    fun isSuggestionsLoading(link: String): Boolean = _suggestionsLoading[link] == true

    /**
     * Detecta o domínio mais frequente entre os sources dos resultados,
     * dispara uma busca refinada "query site:dominio.com" e retorna
     * o resultado embaralhado como sugestões.
     *
     * Cacheia por link para não refazer a busca de rede ao reabrir a mesma imagem.
     */
    fun loadRefinedSuggestions(
        link: String,
        baseQuery: String,
        allImages: List<SearchItem>,
        onResult: (List<SearchItem>) -> Unit
    ) {
        _refinedSuggestionsCache[link]?.let {
            onResult(it)
            return
        }

        val dominantDomain = findDominantDomain(allImages)
        if (dominantDomain == null || baseQuery.isBlank()) {
            // Sem domínio identificável — usa fallback local embaralhado
            val fallback = allImages.distinctBy { it.link }.filter { it.link != link }.shuffled().take(20)
            _refinedSuggestionsCache[link] = fallback
            onResult(fallback)
            return
        }

        _suggestionsLoading[link] = true
        viewModelScope.launch {
            val refined = repository.fetchRefinedByDomain(baseQuery, dominantDomain)
                .distinctBy { it.link }
                .filter { it.link != link }
                .shuffled()
                .take(20)

            val result = refined.ifEmpty {
                allImages.distinctBy { it.link }.filter { it.link != link }.shuffled().take(20)
            }

            _refinedSuggestionsCache[link] = result
            _suggestionsLoading[link] = false
            onResult(result)
        }
    }

    /**
     * Extrai o domínio mais frequente entre os sources (ex: "zerochan", "deviantart")
     * e mapeia para um domínio completo plausível para usar com site:.
     * O source da API já costuma vir como o domínio raiz em minúsculas.
     */
    private fun findDominantDomain(allImages: List<SearchItem>): String? {
        val frequency = allImages
            .map { it.source }
            .filter { it.isNotBlank() && it != "unknown" }
            .groupingBy { it }
            .eachCount()

        val mostCommon = frequency.maxByOrNull { it.value }?.key ?: return null

        // Garante que tem formato de domínio (com ponto), senão assume .com
        return if (mostCommon.contains(".")) mostCommon else "$mostCommon.com"
    }

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
        _refinedSuggestionsCache.clear()
        _suggestionsLoading.clear()
        loadedKeysVersion = 0
        errorKeysVersion = 0
        _query.value = newQuery
    }
}