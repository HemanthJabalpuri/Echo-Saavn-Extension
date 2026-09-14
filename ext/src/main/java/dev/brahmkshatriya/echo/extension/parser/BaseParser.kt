package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Artist

import kotlinx.serialization.json.*

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
    
    protected fun parseArtistsFromArtistMap(artistMap: JsonObject?): List<Artist> {
        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray ?: return emptyList()
        
        return primaryArtists.mapNotNull { element ->
            val obj = element.jsonObject
            val permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: ""
            val numericId = obj["id"]?.jsonPrimitive?.content ?: ""
            val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            
            val artistId = if (permaUrl.isNotBlank()) {
                permaUrl.substringAfterLast("/")
            } else {
                numericId
            }
            if (artistId.isBlank()) return@mapNotNull null
            
            Artist(
                id = artistId,
                name = decodeHtml(name),
                extras = mapOf(
                    "permaUrl" to permaUrl,
                    "artistId" to numericId
                )
            )
        }
    }
}
