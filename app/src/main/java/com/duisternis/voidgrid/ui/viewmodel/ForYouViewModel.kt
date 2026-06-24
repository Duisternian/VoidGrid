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
            pagingData.filter { item -> seenLinks.add(item.link) }
        }
        .cachedIn(viewModelScope)

    fun updateQueriesFromPins(pins: List<PinEntity>, folders: List<FolderEntity>) {
        if (pins.isEmpty()) {
            _activeQueries.value = emptyList()
            seenLinks.clear()
            return
        }
        val queries = buildQueries(pins, folders)
        if (queries != _activeQueries.value) _activeQueries.value = queries
    }

    fun refresh(pins: List<PinEntity>, folders: List<FolderEntity>) {
        if (pins.isEmpty()) return
        seenLinks.clear()
        rotationSeed++
        _activeQueries.value = emptyList()
        _activeQueries.value = buildQueries(pins, folders)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE: Geração de queries
    //
    // Hierarquia por pasta:
    //   1. sourceQuery + folder.categories  → caso ideal e mais comum
    //   2. folder.categories sozinho        → pin sem query (import externo etc.)
    //   3. fallbackQuery()                  → último recurso
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildQueries(
        pins: List<PinEntity>,
        folders: List<FolderEntity>
    ): List<ForYouQuery> {
        val folderMap = folders.associateBy { it.id }
        val byFolder = pins.groupBy { it.folderId }
        val rng = Random(rotationSeed.toLong())
        val queries = mutableListOf<ForYouQuery>()

        byFolder.forEach { (folderId, folderPins) ->
            val folder = folderMap[folderId]
            val categories = folder?.categoryList() ?: emptyList()

            // Pega um pin com sourceQuery, embaralhado para variar a cada refresh
            val pin = folderPins.shuffled(rng).firstOrNull { !it.sourceQuery.isNullOrBlank() }

            val query = when {
                // Nível 1 — sourceQuery + categorias
                pin != null && categories.isNotEmpty() -> ForYouQuery(
                    text = assembleQuery(pin.sourceQuery!!, categories),
                    colorFilter = pin.dominantColor
                )
                // Nível 1b — sourceQuery sem categorias
                pin != null -> ForYouQuery(
                    text = pin.sourceQuery!!,
                    colorFilter = pin.dominantColor
                )
                // Nível 2 — categorias sozinhas (pin sem query)
                categories.isNotEmpty() -> ForYouQuery(
                    text = categories.take(4).joinToString(" "),
                    colorFilter = null
                )
                // Nível 3 — fallback
                else -> ForYouQuery(text = fallbackQuery())
            }

            queries.add(query)
        }

        // Preenche slots restantes (até 4) com pins de pastas diferentes,
        // variando a query escolhida para diversificar o feed
        val slotsLeft = 4 - queries.size
        if (slotsLeft > 0) {
            pins.shuffled(Random(rotationSeed.toLong() + 1))
                .mapNotNull { pin ->
                    val folder = folderMap[pin.folderId]
                    val categories = folder?.categoryList() ?: emptyList()
                    if (pin.sourceQuery.isNullOrBlank() && categories.isEmpty()) return@mapNotNull null
                    val text = when {
                        !pin.sourceQuery.isNullOrBlank() && categories.isNotEmpty() ->
                            assembleQuery(pin.sourceQuery, categories)
                        !pin.sourceQuery.isNullOrBlank() -> pin.sourceQuery
                        else -> categories.take(4).joinToString(" ")
                    }
                    ForYouQuery(text = text, colorFilter = pin.dominantColor)
                        .takeIf { candidate -> queries.none { it.text == candidate.text } }
                }
                .distinctBy { it.text }
                .take(slotsLeft)
                .forEach { queries.add(it) }
        }

        return queries.distinctBy { it.text }.ifEmpty { listOf(ForYouQuery(fallbackQuery())) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Monta a query final: sourceQuery + até 3 categorias da pasta.
     * Ex: "mork borg" + [rpg, art, scenario] → "mork borg rpg art scenario"
     */
    private fun assembleQuery(sourceQuery: String, categories: List<String>): String {
        val cats = categories.take(3).joinToString(" ")
        return "$sourceQuery $cats".trim()
    }

    private fun fallbackQuery(): String = listOf(
        "digital art",
        "aesthetic photography",
        "concept art",
        "illustration art",
        "nature photography",
        "architecture design"
    ).random()
}