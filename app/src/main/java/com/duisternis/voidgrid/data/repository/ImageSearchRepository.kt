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
    // ‘Token’ VQD cacheado por query — cada query tem o seu próprio ‘token’
    private var cachedQuery: String? = null
    private var vqdToken: String? = null

    // Cache separado de ‘tokens’ para buscas refinadas (site:dominio.com)
    // Evita misturar com o ‘token’ da busca principal
    private val refinedVqdCache = mutableMapOf<String, String>()

    suspend fun fetchImages(query: String, position: Int): Pair<List<SearchItem>, Int?> {
        return try {
            if (query != cachedQuery) {
                vqdToken = null
                cachedQuery = query
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
            val json = api.getImagesJson(query, vqd, position)
            parser.parse(json)
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro ao buscar imagens — query='$query' position=$position", e)
            Pair(emptyList(), null)
        }
    }

    fun searchImages(query: String): Flow<PagingData<SearchItem>> =
        Pager(
            config = PagingConfig(
                pageSize = 15,
                initialLoadSize = 15,
                prefetchDistance = 8,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SearchPagingSource(this, query) }
        ).flow

    /**
     * Busca refinada por domínio (ex: "blame! Manga site:zerochan.‘net’").
     * Usa cache de ‘token’ próprio, mais leve — pede só 1 página com poucos itens
     * já que o resultado vai ser embaralhado e usado como sugestão.
     */
    suspend fun fetchRefinedByDomain(baseQuery: String, domain: String): List<SearchItem> {
        val refinedQuery = "$baseQuery site:$domain"
        return try {
            val vqd = refinedVqdCache.getOrPut(refinedQuery) {
                val html = api.getVqdToken(refinedQuery)
                """vqd=["']?([0-9-]+)["']?""".toRegex()
                    .find(html)?.groupValues?.getOrNull(1)
                    ?: return emptyList()
            }

            val json = api.getImagesJson(refinedQuery, vqd, skip = 0)
            val (items, _) = parser.parse(json)
            items
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro na busca refinada por domínio — query='$refinedQuery'", e)
            emptyList()
        }
    }
}