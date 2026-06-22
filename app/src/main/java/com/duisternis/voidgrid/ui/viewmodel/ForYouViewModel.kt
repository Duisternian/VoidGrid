package com.duisternis.voidgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.duisternis.voidgrid.data.api.MultiQueryPagingSource
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.ForYouQuery
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.net.URI
import java.util.Random

class ForYouViewModel(
    private val repository: ImageSearchRepository
) : ViewModel() {

    private val _activeQueries = MutableStateFlow<List<ForYouQuery>>(emptyList())
    private var rotationSeed = 0

    // Rastreador de links únicos para evitar duplicatas no feed
    private val seenLinks = mutableSetOf<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _activeQueries
        .flatMapLatest { queries ->
            if (queries.isEmpty()) flowOf(PagingData.empty())
            else Pager(
                config = PagingConfig(
                    pageSize = 30,
                    initialLoadSize = 30,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    MultiQueryPagingSource(
                        repository = repository,
                        queries = queries,
                        safeSearch = false
                    )
                }
            ).flow
        }
        .map { pagingData ->
            // Filtra duplicatas comparando o link do item com o Set de links vistos
            pagingData.filter { item ->
                seenLinks.add(item.link)
            }
        }
        .cachedIn(viewModelScope)

    fun updateQueriesFromPins(pins: List<PinEntity>, folders: List<FolderEntity>) {
        if (pins.isEmpty()) {
            _activeQueries.value = emptyList()
            seenLinks.clear()
            return
        }
        val queries = buildQueriesPerPin(pins, folders)
        if (queries != _activeQueries.value) _activeQueries.value = queries
    }

    fun refresh(pins: List<PinEntity>, folders: List<FolderEntity>) {
        if (pins.isEmpty()) return

        // Limpa o histórico de links ao refrescar para permitir novas descobertas
        seenLinks.clear()

        rotationSeed++
        _activeQueries.value = emptyList()
        _activeQueries.value = buildQueriesPerPin(pins, folders)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE: Geração de queries (Mantido como estava)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildQueriesPerPin(
        pins: List<PinEntity>,
        folders: List<FolderEntity>
    ): List<ForYouQuery> {
        val folderMap = folders.associateBy { it.id }
        val byFolder = pins.groupBy { it.folderId }
        val rng = Random(rotationSeed.toLong())
        val queries = mutableListOf<ForYouQuery>()

        byFolder.forEach { (folderId, folderPins) ->
            val folderName = folderMap[folderId]?.name ?: ""
            val query = buildQueryForFolder(folderPins.shuffled(rng), folderName)
            if (query != null) {
                queries.add(query)
            }
        }

        val slotsLeft = 4 - queries.size
        if (slotsLeft > 0) {
            pins.shuffled(Random(rotationSeed.toLong() + 1))
                .mapNotNull { pin ->
                    val folderName = folderMap[pin.folderId]?.name ?: ""
                    buildQueryForPin(pin, folderName)
                        ?.takeIf { candidate -> queries.none { it.text == candidate.text } }
                }
                .distinctBy { it.text }
                .take(slotsLeft)
                .forEach { queries.add(it) }
        }

        return queries.distinctBy { it.text }.ifEmpty { listOf(ForYouQuery(fallbackQuery())) }
    }

    private fun buildQueryForFolder(folderPins: List<PinEntity>, folderName: String): ForYouQuery? {
        for (pin in folderPins) {
            val query = pin.sourceQuery
            if (!query.isNullOrBlank()) return ForYouQuery(text = query, colorFilter = pin.dominantColor)
        }
        for (pin in folderPins) {
            val linkKeywords = extractKeywordsFromImageUrl(pin.link)
            if (linkKeywords.isNotEmpty()) {
                val tags = getTopTags(listOf(pin), limit = 2)
                return ForYouQuery(text = assembleQuery(linkKeywords, tags, mapSourceDomain(pin.source)), colorFilter = pin.dominantColor)
            }
        }
        val allTags = getTopTags(folderPins, limit = 4)
        if (allTags.isNotEmpty()) {
            val source = folderPins.firstNotNullOfOrNull { mapSourceDomain(it.source).takeIf { s -> s.isNotBlank() } } ?: ""
            return ForYouQuery(text = assembleQuery(emptyList(), allTags, source), colorFilter = folderPins.firstOrNull { !it.dominantColor.isNullOrBlank() }?.dominantColor)
        }
        val folderText = mapFolderName(folderName).takeIf { it.isNotBlank() } ?: return null
        return ForYouQuery(text = folderText, colorFilter = null)
    }

    private fun buildQueryForPin(pin: PinEntity, folderName: String): ForYouQuery? {
        val sourceQuery = pin.sourceQuery
        if (!sourceQuery.isNullOrBlank()) return ForYouQuery(text = sourceQuery, colorFilter = pin.dominantColor)

        val linkKeywords = extractKeywordsFromImageUrl(pin.link)
        val tags = getTopTags(listOf(pin), limit = 3)
        val source = mapSourceDomain(pin.source)

        if (linkKeywords.isNotEmpty()) return ForYouQuery(text = assembleQuery(linkKeywords, tags, source), colorFilter = pin.dominantColor)
        if (tags.isNotEmpty()) return ForYouQuery(text = assembleQuery(emptyList(), tags, source), colorFilter = pin.dominantColor)

        val folderText = mapFolderName(folderName).takeIf { it.isNotBlank() } ?: return null
        return ForYouQuery(text = folderText, colorFilter = null)
    }

    private fun assembleQuery(linkKeywords: List<String>, tags: List<String>, sourceContext: String): String {
        return when {
            linkKeywords.isNotEmpty() && tags.isNotEmpty() -> "${linkKeywords.take(3).joinToString(" ")} ${tags.take(2).joinToString(" ")}"
            linkKeywords.isNotEmpty() -> linkKeywords.take(4).joinToString(" ")
            tags.isNotEmpty() && sourceContext.isNotBlank() -> "$sourceContext ${tags.take(3).joinToString(" ")}"
            tags.isNotEmpty() -> tags.take(4).joinToString(" ")
            sourceContext.isNotBlank() -> sourceContext
            else -> fallbackQuery()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES (Mantidos conforme original)
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractKeywordsFromImageUrl(url: String): List<String> {
        if (url.isBlank() || isProxyOrCdnUrl(url)) return emptyList()
        return try {
            val uri = URI(url)
            val raw = "${uri.path ?: ""} ${uri.query ?: ""}"
            raw.split("/", "-", "_", "+", ".", "=", "&", "?", "%20", " ", "%", "x")
                .map { it.lowercase().trim() }
                .filter { word ->
                    word.length in 4..20 && word !in IMAGE_URL_NOISE && !word.all { it.isDigit() }
                            && !word.matches(Regex("[a-f0-9]{6,}")) && !word.matches(Regex(".*\\d{3,}.*"))
                            && word !in setOf("true", "false", "null")
                }
                .distinct()
                .take(4)
        } catch (e: Exception) { emptyList() }
    }

    private fun isProxyOrCdnUrl(url: String): Boolean {
        val lower = url.lowercase()
        return PROXY_CDN_HOSTS.any { lower.contains(it) } || lower.contains("/th/id/") || lower.contains("/th?id=")
                || lower.matches(Regex(".*[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}.*"))
    }

    private val PROXY_CDN_HOSTS = setOf("tse1.mm.bing", "tse2.mm.bing", "tse3.mm.bing", "tse4.mm.bing", "th.bing.com", "i.imgur.com", "i.redd.it", "external-preview.redd.it", "preview.redd.it", "pbs.twimg.com", "media.discordapp", "cdn.discordapp", "images-wixmp", "akamaihd.net", "fbcdn.net", "googleusercontent.com", "gstatic.com", "pinimg.com", "media.tenor.com", "c.tenor.com", "cdn.kobo.com", "images-na.ssl-images-amazon.com", "m.media-amazon.com", "covers.openlibrary.org", "books.google.com", "encrypted-tbn")

    private val IMAGE_URL_NOISE = setOf("www", "com", "net", "org", "html", "http", "https", "cdn", "static", "assets", "media", "upload", "uploads", "file", "files", "content", "storage", "blob", "cache", "edge", "imgix", "cloudfront", "image", "images", "photo", "photos", "thumb", "thumbnail", "large", "small", "medium", "full", "orig", "original", "preview", "jpeg", "webp", "png", "jpg", "gif", "avif", "reddit", "pinterest", "tumblr", "twitter", "instagram", "facebook", "danbooru", "gelbooru", "pixiv", "deviantart", "artstation", "myanimelist", "anilist", "mangadex", "zerochan", "wiki", "fandom", "wikia", "wikipedia", "bing", "duckduckgo", "google", "yahoo", "posts", "post", "index", "page", "view", "item", "show", "tags", "search", "query", "result", "results", "users", "user", "with", "from", "that", "this", "your", "their", "about", "book", "books", "cover", "covers", "false", "true", "null", "undefined", "width", "height", "resize", "scale", "crop", "format", "2024", "2023", "2022", "2021", "2020", "2019")

    private fun mapSourceDomain(source: String): String {
        val domain = source.lowercase().trim()
        return when {
            domain.contains("pixiv") -> "digital illustration"
            domain.contains("artstation") -> "concept art illustration"
            domain.contains("deviantart") -> "digital art fanart"
            domain.contains("danbooru") || domain.contains("gelbooru") || domain.contains("zerochan") -> "anime illustration"
            domain.contains("myanimelist") || domain.contains("anilist") -> "anime art"
            domain.contains("mangadex") -> "manga art"
            else -> ""
        }
    }

    private fun getTopTags(pins: List<PinEntity>, limit: Int): List<String> {
        return pins.flatMap { it.tags.split(",").map { t -> t.trim().lowercase() } }.filter { it.length > 2 }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    private fun mapFolderName(name: String): String {
        if (name.isBlank()) return ""
        val normalized = name.lowercase().trim()
        FOLDER_SEMANTIC_MAP[normalized]?.let { return it }
        val segments = normalized.split("/", "-", "_", " ", ",", ".")
        val matches = segments.mapNotNull { FOLDER_SEMANTIC_MAP[it.trim()] }
        return when {
            matches.size >= 2 -> matches.take(2).joinToString(" ")
            matches.size == 1 -> matches.first()
            else -> normalized.filter { it.isLetter() || it.isWhitespace() }.trim().takeIf { it.length > 2 } ?: ""
        }
    }

    private val FOLDER_SEMANTIC_MAP = mapOf("manga" to "manga", "m" to "manga", "mangás" to "manga", "mangas" to "manga", "anime" to "anime", "a" to "anime", "animes" to "anime", "seinen" to "seinen manga", "shonen" to "shonen manga", "shoujo" to "shoujo manga", "isekai" to "isekai anime", "mecha" to "mecha anime", "webtoon" to "webtoon manhwa", "rpg" to "rpg tabletop", "r" to "rpg tabletop", "games" to "video game", "game" to "video game", "gaming" to "gaming", "jrpg" to "jrpg fantasy", "souls" to "soulslike game", "fps" to "fps game", "indie" to "indie game", "ttrpg" to "tabletop rpg", "tabletop" to "tabletop rpg", "art" to "digital art", "arte" to "digital art", "illustration" to "illustration", "ilustração" to "illustration", "concept" to "concept art", "fanart" to "fan art", "pixelart" to "pixel art", "pixel" to "pixel art", "wallpaper" to "wallpaper", "wallpapers" to "wallpaper", "w" to "wallpaper", "aesthetic" to "aesthetic", "dark" to "dark aesthetic", "lofi" to "lofi aesthetic", "vaporwave" to "vaporwave", "cyberpunk" to "cyberpunk", "fantasy" to "fantasy art", "nature" to "nature photography", "landscape" to "landscape photography", "horror" to "horror art", "sci-fi" to "sci-fi art", "scifi" to "sci-fi art", "medieval" to "medieval fantasy art", "gothic" to "gothic art", "minimalist" to "minimalist design", "photography" to "photography", "foto" to "photography", "fotos" to "photography", "portrait" to "portrait photography", "retrato" to "portrait photography", "cars" to "cars", "carros" to "cars", "motos" to "motorcycle", "food" to "food photography", "comida" to "food photography", "comidas" to "food photography", "travel" to "travel photography", "viagem" to "travel photography", "fashion" to "fashion", "moda" to "fashion", "architecture" to "architecture", "arquitetura" to "architecture", "space" to "space astronomy", "espaço" to "space astronomy", "music" to "music", "música" to "music", "sport" to "sport", "esporte" to "sport", "esportes" to "sport", "favoritos" to "", "favs" to "", "saved" to "", "misc" to "", "outros" to "", "random" to "")

    private fun fallbackQuery(): String = listOf("digital art", "aesthetic photography", "concept art", "illustration art", "nature photography", "architecture design").random()
}