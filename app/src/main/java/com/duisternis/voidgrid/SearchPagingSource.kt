package com.duisternis.voidgrid

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ─── PagingSource para busca de imagens ──────────────────────────────────────

class SearchPagingSource(
    private val query: String,
    private val api: GoogleSearchApi
) : PagingSource<Int, SearchItem>() {

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private var cachedVqd: String? = null

    // ─── Carregamento de dados ────────────────────────────────────────────────

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchItem> {
        val position = params.key ?: 0

        return try {
            val vqd = cachedVqd ?: fetchVqdToken() ?: return LoadResult.Error(Exception("Token VQD indisponível"))

            val json = api.getImagesJson(query, vqd, position)
            val (items, nextS) = parseDuckDuckGoJson(json)

            LoadResult.Page(
                data = items,
                prevKey = if (position == 0) null else position,
                nextKey = nextS
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun fetchVqdToken(): String? = try {
        val html = RetrofitClient.duckDuckGoApi.getVqdToken(query)
        val extracted = """vqd=["']?([0-9-]+)["']?""".toRegex().find(html)?.groupValues?.getOrNull(1)
        cachedVqd = extracted
        extracted
    } catch (_: Exception) {
        null
    }

    override fun getRefreshKey(state: PagingState<Int, SearchItem>): Int? = state.anchorPosition

    // ─── Processamento JSON ───────────────────────────────────────────────────

    private fun parseDuckDuckGoJson(jsonString: String): Pair<List<SearchItem>, Int?> {
        return try {
            val root = jsonParser.parseToJsonElement(jsonString).jsonObject

            val nextS = root["next"]?.jsonPrimitive?.content?.let { url ->
                Regex("""s=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }

            val items = root["results"]?.jsonArray?.mapNotNull { element ->
                val obj = element.jsonObject
                val link = obj["image"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val source = obj["source"]?.jsonPrimitive?.content?.lowercase() ?: "unknown"
                val width = obj["width"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val height = obj["height"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                if (link.startsWith("http")) SearchItem(link, source, width, height) else null
            } ?: emptyList()

            Pair(items, nextS)
        } catch (_: Exception) {
            Pair(emptyList(), null)
        }
    }
}