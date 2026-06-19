package com.duisternis.voidgrid.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.local.ProviderPreferences
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.model.SearchProvider
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ImageSearchViewModel(
    private val repository: ImageSearchRepository,
    private val providerPreferences: ProviderPreferences
) : ViewModel() {

    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    private val _customDomains = MutableStateFlow<List<String>>(emptyList())
    val customDomains: StateFlow<List<String>> = _customDomains.asStateFlow()

    private val _selectedProvider = MutableStateFlow<SearchProvider>(SearchProvider.All)
    val selectedProvider: StateFlow<SearchProvider> = _selectedProvider.asStateFlow()

    // SafeSearch — ‘default’ ligado, persistido no DataStore
    private val _safeSearch = MutableStateFlow(true)
    val safeSearch: StateFlow<Boolean> = _safeSearch.asStateFlow()

    init {
        providerPreferences.customDomains
            .onEach { _customDomains.value = it }
            .launchIn(viewModelScope)

        combine(
            providerPreferences.selectedProviderId,
            providerPreferences.customDomains
        ) { id, domains -> SearchProvider.fromId(id, domains) }
            .onEach { _selectedProvider.value = it }
            .launchIn(viewModelScope)

        // Restaura o estado de safe search da última sessão
        providerPreferences.safeSearch
            .onEach { _safeSearch.value = it }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = combine(_query, _selectedProvider, _safeSearch) { text, provider, safe ->
        text?.let { Pair(buildEffectiveQuery(it, provider), safe) }
    }
        .filterNotNull()
        .filter { it.first.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { (query, safe) -> repository.searchImages(query, safe) }
        .cachedIn(viewModelScope)

    private fun buildEffectiveQuery(
        text: String,
        provider: SearchProvider
    ): String {
        // safeSearch é controlado pelo parâmetro p= na URL da API, não no texto
        var query = text
        provider.siteFilter?.let { query = "$query $it" }
        return query
    }

    fun toggleSafeSearch() {
        val new = !_safeSearch.value
        _safeSearch.value = new
        viewModelScope.launch {
            providerPreferences.setSafeSearch(new)
        }
        // Refaz a busca com o novo filtro
        clearSearchCaches()
        // Força o combine a reemitir disparando com o mesmo _query
        _query.value = _query.value
    }

    fun selectProvider(provider: SearchProvider) {
        if (provider == _selectedProvider.value) return
        clearSearchCaches()
        _selectedProvider.value = provider
        viewModelScope.launch {
            providerPreferences.setSelectedProviderId(provider.id)
        }
    }

    fun addAndSelectCustomDomain(rawDomain: String) {
        if (rawDomain.isBlank()) return
        viewModelScope.launch {
            providerPreferences.addCustomDomain(rawDomain)
        }
        val normalized = rawDomain.trim()
            .removePrefix("https://").removePrefix("http://").removePrefix("www.")
            .substringBefore("/").lowercase()
        selectProvider(SearchProvider.Custom(normalized))
    }

    fun removeCustomDomain(domain: String) {
        viewModelScope.launch {
            providerPreferences.removeCustomDomain(domain)
        }
        if (_selectedProvider.value.id == "custom:$domain") {
            selectProvider(SearchProvider.All)
        }
    }

    private val _colorCache = mutableStateMapOf<String, Color>()
    val colorCache: Map<String, Color> get() = _colorCache

    fun cacheColor(thumbnailUrl: String, color: Color) {
        _colorCache[thumbnailUrl] = color
    }

    private val _transparentKeys = mutableStateMapOf<String, Boolean>()

    fun markTransparent(link: String) {
        _transparentKeys[link] = true
    }

    fun isTransparent(link: String): Boolean = _transparentKeys[link] == true

    private val _refinedSuggestionsCache = mutableMapOf<String, List<SearchItem>>()

    private val _suggestionsLoading = mutableStateMapOf<String, Boolean>()
    fun isSuggestionsLoading(link: String): Boolean = _suggestionsLoading[link] == true

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

    // Domínios a ignorar — são intermediários/buscadores, não fontes de conteúdo
    private val blockedDomains = setOf(
        "bing.com", "microsoft.com", "google.com", "duckduckgo.com",
        "yahoo.com", "gstatic.com", "googleusercontent.com", "msn.com",
        "wikipedia.org", "wikimedia.org", "amazonaws.com", "cloudfront.net",
        "imgur.com", "i.imgur.com"
    )

    private fun findDominantDomain(allImages: List<SearchItem>): String? {
        val frequency = allImages
            .map { it.source }
            .filter { source ->
                source.isNotBlank()
                        && source != "unknown"
                        && blockedDomains.none { blocked -> source.contains(blocked) }
            }
            .groupingBy { it }
            .eachCount()

        val mostCommon = frequency.maxByOrNull { it.value }?.key ?: return null
        return if (mostCommon.contains(".")) mostCommon else "$mostCommon.com"
    }

    private val _loadedKeys = mutableSetOf<String>()
    private val _errorKeys = mutableSetOf<String>()

    var loadedKeysVersion by mutableIntStateOf(0)
        private set
    var errorKeysVersion by mutableIntStateOf(0)
        private set

    fun markLoaded(link: String) {
        if (_loadedKeys.add(link)) loadedKeysVersion++
    }

    fun markError(link: String) {
        if (_errorKeys.add(link)) errorKeysVersion++
    }

    fun isError(link: String): Boolean {
        @Suppress("UNUSED_EXPRESSION") errorKeysVersion
        return link in _errorKeys
    }

    fun search(newQuery: String) {
        if (newQuery.isBlank() || newQuery == _query.value) return
        clearSearchCaches()
        _query.value = newQuery
    }

    private fun clearSearchCaches() {
        _colorCache.clear()
        _loadedKeys.clear()
        _errorKeys.clear()
        _transparentKeys.clear()
        _refinedSuggestionsCache.clear()
        _suggestionsLoading.clear()
        loadedKeysVersion = 0
        errorKeysVersion = 0
    }
}