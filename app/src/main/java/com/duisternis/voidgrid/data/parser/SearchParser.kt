package com.duisternis.voidgrid.data.parser

import com.duisternis.voidgrid.data.model.SearchItem
import kotlinx.serialization.json.*

class SearchParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): Pair<List<SearchItem>, Int?> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val nextS = root["next"]?.jsonPrimitive?.content?.let { url ->
                Regex("""s=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            val items = root["results"]?.jsonArray?.mapNotNull { element ->
                val obj = element.jsonObject
                val link = obj["image"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val source = obj["source"]?.jsonPrimitive?.content?.lowercase() ?: "unknown"
                val width = obj["width"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val height = obj["height"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val thumbnail = obj["thumbnail"]?.jsonPrimitive?.content
                    ?.takeIf { it.startsWith("http") }
                val title = obj["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                if (link.startsWith("http")) SearchItem(link, source, width, height, thumbnail, title) else null
            } ?: emptyList()
            Pair(items, nextS)
        } catch (e: Exception) {
            logger("Erro ao parser JSON — primeiros 200 chars: ${jsonString.take(200)}\n${e.message}")
            Pair(emptyList(), null)
        }
    }

    private fun logger(message: String) {
        try {
            android.util.Log.e("SearchParser", message)
        } catch (_: RuntimeException) {
            println("[SearchParser] $message")
        }
    }
}