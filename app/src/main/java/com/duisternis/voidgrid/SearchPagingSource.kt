package com.duisternis.voidgrid

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SearchPagingSource(
    private val query: String,
    private val api: GoogleSearchApi,
    private val tokenCache: TokenCache
) : PagingSource<Int, SearchItem>() {

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private var cachedVqd: String? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchItem> {
        val position = params.key ?: 0
        return try {
            val vqd = cachedVqd ?: run {
                try {
                    tokenCache.getOrFetch(query) {
                        RetrofitClient.htmlApi.getVqdToken(query)
                    }.also { cachedVqd = it }
                } catch (e: Exception) {
                    return LoadResult.Error(e)
                }
            }

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

    override fun getRefreshKey(state: PagingState<Int, SearchItem>): Int? = null

    private fun parseDuckDuckGoJson(jsonString: String): Pair<List<SearchItem>, Int?> {
        return try {
            val jsonObject = jsonParser.parseToJsonElement(jsonString).jsonObject

            val nextUrl = jsonObject["next"]?.jsonPrimitive?.content
            val nextS = nextUrl?.let {
                Regex("""s=(\d+)""").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }

            val resultsArray = jsonObject["results"]?.jsonArray ?: return Pair(emptyList(), null)
            val items = resultsArray.mapNotNull { element ->
                val obj = element.jsonObject
                val link = obj["image"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val source = obj["source"]?.jsonPrimitive?.content?.lowercase() ?: "unknown"
                SearchItem(link, source)
            }.filter { it.link.startsWith("http") }

            Pair(items, nextS)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }
}