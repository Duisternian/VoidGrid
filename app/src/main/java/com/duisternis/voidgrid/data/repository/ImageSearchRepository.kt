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
    // Token VQD cacheado por query — cada query tem seu próprio token
    private var cachedQuery: String? = null
    private var vqdToken: String? = null

    suspend fun fetchImages(query: String, position: Int): Pair<List<SearchItem>, Int?> {
        return try {
            // Invalida o token se a query mudou
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
                // Com o spinner de "carregando mais" agora visível no fim da
                // grid, não precisamos de um prefetch tão alto quanto antes —
                // o objetivo deixou de ser "nunca travar" e passou a ser
                // "quando travar, mostrar que tá carregando" (como o Cosmos).
                // 8 dá uma margem pequena sem fazer o spinner praticamente
                // nunca aparecer.
                prefetchDistance = 8,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SearchPagingSource(this, query) }
        ).flow
}