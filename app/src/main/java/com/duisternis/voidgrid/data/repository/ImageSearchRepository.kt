package com.duisternis.voidgrid.data.repository

import android.util.Log
import com.duisternis.voidgrid.data.api.DuckDuckGoApi
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.parser.SearchParser

class ImageSearchRepository(
    private val api: DuckDuckGoApi,
    private val parser: SearchParser
) {
    private var vqdToken: String? = null

    suspend fun fetchImages(query: String, position: Int): Pair<List<SearchItem>, Int?> {
        return try {
            if (vqdToken == null) {
                val html = api.getVqdToken(query)
                vqdToken = """vqd=["']?([0-9-]+)["']?""".toRegex().find(html)?.groupValues?.getOrNull(1)
            }

            val vqd = vqdToken ?: return Pair(emptyList(), null)
            val json = api.getImagesJson(query, vqd, position)
            parser.parse(json)
        } catch (e: Exception) {
            Log.e("ImageSearchRepository", "Erro na busca: ${e.message}")
            Pair(emptyList(), null)
        }
    }
}