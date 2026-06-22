package com.duisternis.voidgrid.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.duisternis.voidgrid.data.api.DuckDuckGoApi
import com.duisternis.voidgrid.data.api.SearchPagingSource
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.parser.SearchParser
import kotlinx.coroutines.flow.Flow

class ImageSearchRepository(
    private val api: DuckDuckGoApi,
    private val parser: SearchParser
) {
    private var cachedQueryKey: String? = null
    private var vqdToken: String? = null
    private val refinedVqdCache = mutableMapOf<String, String>()

    suspend fun fetchImages(
        query: String,
        position: Int,
        safeSearch: Boolean,
        colorFilter: String? = null
    ): Pair<List<SearchItem>, Int?> {
        return try {
            val queryKey = "$query|safe=$safeSearch|color=$colorFilter"
            if (queryKey != cachedQueryKey) {
                vqdToken = null
                cachedQueryKey = queryKey
            }

            if (vqdToken == null) {
                val html = api.getVqdToken(query)
                vqdToken = """vqd=["']?([0-9-]+)["']?""".toRegex()
                    .find(html)?.groupValues?.getOrNull(1)
            }

            val vqd = vqdToken ?: return Pair(emptyList(), null)
            val safeParam = if (safeSearch) "1" else "-1"
            val filtersParam = colorFilter?.let { ",,,,,color:$it" } ?: ",,,,,"

            // CORREÇÃO: O argumento nomeado agora é 'skip' para coincidir com a interface
            val json = api.getImagesJson(
                query = query,
                vqd = vqd,
                skip = position,
                safeSearch = safeParam,
                filters = filtersParam
            )
            parser.parse(json)
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro ao buscar imagens", e)
            Pair(emptyList(), null)
        }
    }

    fun searchImages(query: String, safeSearch: Boolean): Flow<PagingData<SearchItem>> =
        Pager(
            config = PagingConfig(
                pageSize = 15,
                initialLoadSize = 15,
                prefetchDistance = 8,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SearchPagingSource(this, query, safeSearch) }
        ).flow

    suspend fun fetchRefinedByDomain(baseQuery: String, domain: String, safeSearch: Boolean = true): List<SearchItem> {
        val refinedQuery = "$baseQuery site:$domain"
        return try {
            val vqd = refinedVqdCache.getOrPut(refinedQuery) {
                val html = api.getVqdToken(refinedQuery)
                """vqd=["']?([0-9-]+)["']?""".toRegex()
                    .find(html)?.groupValues?.getOrNull(1)
                    ?: return emptyList()
            }

            val safeParam = if (safeSearch) "1" else "-1"

            // Ajustado para manter compatibilidade com a interface
            val json = api.getImagesJson(
                query = refinedQuery,
                vqd = vqd,
                skip = 0,
                safeSearch = safeParam
            )
            val (items, _) = parser.parse(json)
            items
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro na busca refinada", e)
            emptyList()
        }
    }
}