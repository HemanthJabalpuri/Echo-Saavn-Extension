package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.convertImageUrl

open class BaseParser {
    
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    protected fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    protected fun parseArtistFromJson(obj: JsonObject): Artist? {
        // Try search format first, fallback to detail format
        val permaUrl = obj["perma_url"]?.jsonPrimitive?.content
            ?: obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content
            ?: ""
        val numericId = obj["id"]?.jsonPrimitive?.content
            ?: obj["artistId"]?.jsonPrimitive?.content
            ?: ""
        val id = if (permaUrl.isNotBlank()) permaUrl.substringAfterLast("/") else numericId
        if (id.isBlank()) return null

        return Artist(
            id = id,
            name = decodeHtml(
                obj["name"]?.jsonPrimitive?.content
                    ?: obj["title"]?.jsonPrimitive?.content
                    ?: ""
            ),
            cover = convertImageUrl(obj["image"]?.jsonPrimitive?.content).toImageHolder(),
            subtitle = obj["subtitle"]?.jsonPrimitive?.content
                ?: obj["role"]?.jsonPrimitive?.content,
            extras = mapOf(
                "permaUrl" to permaUrl,
                "artistId" to numericId
            )
        )
    }

    protected fun parseArtistsFromArtistMap(artistMap: JsonObject?): List<Artist> {
        if (artistMap == null) return emptyList()

        val primary = artistMap["primary_artists"]?.jsonArray ?: emptyList()
        val featured = artistMap["featured_artists"]?.jsonArray ?: emptyList()
        val all = artistMap["artists"]?.jsonArray ?: emptyList()

        return (primary + featured + all)
            .distinctBy { it.jsonObject["id"]?.jsonPrimitive?.content }
            .mapNotNull { parseArtistFromJson(it.jsonObject) }
    }

}
