package com.duisternis.voidgrid.data.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.duisternis.voidgrid.data.model.ForYouQuery
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.repository.ImageSearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * PagingSource que busca múltiplas queries em paralelo e intercala os resultados.
 *
 * Cada [ForYouQuery] carrega seu texto de busca e, opcionalmente, um filtro
 * de cor (extraído do pin de origem que gerou aquela query — ver
 * ForYouViewModel.buildQueryForFolder). O filtro de cor é repassado como
 * parâmetro separado à API, nunca embutido no texto da busca.
 *
 * Exemplo com 3 queries (manga, comida, games):
 *   página 0 → busca as 3 queries na posição 0, intercala → [manga, comida, games, manga, comida, games...]
 *   página 1 → busca as 3 queries na posição seguinte de cada uma, intercala novamente
 *
 * O estado de paginação de cada query é rastreado individualmente em [queryPositions].
 */
class MultiQueryPagingSource(
    private val repository: ImageSearchRepository,
    private val queries: List<ForYouQuery>,
    private val safeSearch: Boolean = false
) : PagingSource<MultiQueryPagingSource.MultiQueryKey, SearchItem>() {

    /**
     * Chave de paginação: posição atual de cada query.
     * Ex: [0, 0, 0] → primeira página de cada query
     *     [15, 30, 15] → posições diferentes por query
     */
    data class MultiQueryKey(val positions: List<Int>)

    override suspend fun load(params: LoadParams<MultiQueryKey>): LoadResult<MultiQueryKey, SearchItem> {
        // Posições iniciais (0 para cada query na primeira carga)
        val currentPositions = params.key?.positions ?: List(queries.size) { 0 }

        return try {
            // Busca todas as queries em paralelo
            val results = coroutineScope {
                queries.mapIndexed { index, query ->
                    async {
                        val position = currentPositions.getOrElse(index) { 0 }
                        repository.fetchImages(
                            query = query.text,
                            position = position,
                            safeSearch = safeSearch,
                            colorFilter = query.colorFilter
                        )
                    }
                }.awaitAll()
            }

            // Intercala os resultados: pega 1 de cada query em round-robin
            // [q1[0], q2[0], q3[0], q1[1], q2[1], q3[1], ...]
            val interleaved = interleaveResults(results.map { it.first })

            // Calcula as próximas posições de cada query
            val nextPositions = results.mapIndexed { index, (_, nextPos) ->
                nextPos ?: (currentPositions.getOrElse(index) { 0 } + 15)
            }

            // Só continua paginando se pelo menos uma query ainda tem resultados
            val hasMore = results.any { (items, nextPos) -> items.isNotEmpty() && nextPos != null }

            LoadResult.Page(
                data = interleaved,
                prevKey = null,
                nextKey = if (hasMore) MultiQueryKey(nextPositions) else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * Intercala listas em round-robin.
     * [A, B, C] + [D, E] + [F] → [A, D, F, B, E, C]
     */
    private fun interleaveResults(lists: List<List<SearchItem>>): List<SearchItem> {
        val result = mutableListOf<SearchItem>()
        val maxSize = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxSize) {
            for (list in lists) {
                if (i < list.size) result.add(list[i])
            }
        }
        return result
    }

    override fun getRefreshKey(state: PagingState<MultiQueryKey, SearchItem>): MultiQueryKey? = null
}