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
    // Cache key inclui safeSearch — token gerado com safe=on não serve pra safe=off
    private var cachedQueryKey: String? = null
    private var vqdToken: String? = null
    private val refinedVqdCache = mutableMapOf<String, String>()

    suspend fun fetchImages(query: String, position: Int, safeSearch: Boolean): Pair<List<SearchItem>, Int?> {
        return try {
            val queryKey = "$query|safe=$safeSearch"
            if (queryKey != cachedQueryKey) {
                vqdToken = null
                cachedQueryKey = queryKey
            }

            if (vqdToken == null) {
                val html = api.getVqdToken(query)
                vqdToken = """vqd=["']?([0-9-]+)["']?""".toRegex()
                    .find(html)?.groupValues?.getOrNull(1)
                    .also { token ->
                        if (token == null) Log.w("ImageSearchRepository", "Token VQD não encontrado para query='$query'")
                        else Log.d("ImageSearchRepository", "Token VQD obtido para query='$query'")
                    }
            }

            val vqd = vqdToken ?: return Pair(emptyList(), null)
            val safeParam = if (safeSearch) "1" else "-1"
            val json = api.getImagesJson(query, vqd, position, safeSearch = safeParam)
            parser.parse(json)
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro ao buscar imagens — query='$query' position=$position", e)
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
            val json = api.getImagesJson(refinedQuery, vqd, skip = 0, safeSearch = safeParam)
            val (items, _) = parser.parse(json)
            items
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro na busca refinada — query='$refinedQuery'", e)
            emptyList()
        }
    }
}