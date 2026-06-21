package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ForYouViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _activeQuery = MutableStateFlow<String?>(null)

    // Domínios que são intermediários/buscadores — ignorar como fonte de conteúdo
    private val blockedSources = setOf(
        "bing.com", "microsoft.com", "google.com", "duckduckgo.com",
        "yahoo.com", "gstatic.com", "googleusercontent.com", "msn.com",
        "wikipedia.org", "wikimedia.org", "amazonaws.com", "cloudfront.net",
        "windows.com", "live.com", "office.com", "azure.com"
    )

    // Palavras genéricas demais para virar query
    private val blockedKeywords = setOf(
        "com", "net", "org", "www", "http", "https", "jpg", "jpeg",
        "png", "gif", "webp", "img", "image", "images", "photo", "photos",
        "upload", "uploads", "static", "cdn", "media", "content", "files",
        "file", "thumb", "thumbnail", "large", "small", "medium", "original",
        "download", "preview", "resize", "width", "height", "size", "format",
        "the", "and", "for", "with", "from", "this", "that", "are", "was",
        "00", "01", "02", "03", "04", "05", "1", "2", "3"
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _activeQuery
        .flatMapLatest { query ->
            if (query.isNullOrBlank()) flowOf(PagingData.empty())
            else repository.searchImages(query, safeSearch = false)
        }
        .cachedIn(viewModelScope)

    fun updateQueriesFromPins(pins: List<PinEntity>) {
        if (pins.isEmpty()) {
            _activeQuery.value = null
            return
        }

        val query = buildQueryFromPins(pins)
        if (query != _activeQuery.value) {
            _activeQuery.value = query
        }
    }

    fun refresh(pins: List<PinEntity>) {
        if (pins.isEmpty()) return
        // Força nova query mesmo que seja igual — reseta o paging
        _activeQuery.value = null
        _activeQuery.value = buildQueryFromPins(pins)
    }

    private fun buildQueryFromPins(pins: List<PinEntity>): String {
        // 1. Tenta extrair palavras-chave dos links das imagens salvas
        val keywordsFromLinks = pins
            .flatMap { extractKeywordsFromUrl(it.link) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }

        if (keywordsFromLinks.isNotEmpty()) {
            // Pega 2-3 palavras aleatórias das mais frequentes para variar
            val picked = keywordsFromLinks.shuffled().take(3)
            return picked.joinToString(" ")
        }

        // 2. Fallback: usa fontes não bloqueadas
        val validSources = pins
            .map { it.source }
            .filter { src ->
                src.isNotBlank()
                        && src != "unknown"
                        && blockedSources.none { blocked -> src.contains(blocked) }
            }
            .distinct()

        if (validSources.isNotEmpty()) {
            val domain = validSources.random()
            return "art site:$domain"
        }

        // 3. Último fallback: query genérica de arte/wallpaper
        val genericQueries = listOf(
            "digital art wallpaper",
            "aesthetic photography",
            "concept art illustration",
            "nature photography 4k",
            "minimalist design art"
        )
        return genericQueries.random()
    }

    private fun extractKeywordsFromUrl(url: String): List<String> {
        return try {
            // Remove protocolo e domínio, pega só o path
            val path = url
                .removePrefix("https://")
                .removePrefix("http://")
                .substringAfter("/") // remove domínio
                .substringBeforeLast(".") // remove extensão

            // Divide por separadores comuns em nomes de arquivo
            path
                .split("/", "-", "_", "%20", "+", ".", " ")
                .map { it.lowercase().trim() }
                .filter { word ->
                    word.length in 3..20
                            && word.all { it.isLetter() }
                            && word !in blockedKeywords
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}