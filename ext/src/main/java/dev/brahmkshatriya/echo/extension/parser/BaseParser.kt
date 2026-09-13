package dev.brahmkshatriya.echo.extension.parser

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
    
    protected fun extractArtistNames(array: JsonArray): String {
        return array.mapNotNull {
            it.jsonObject["name"]?.jsonPrimitive?.content
        }.joinToString(", ")
    }
    
    protected fun extractArtistIds(array: JsonArray): String {
        return array.mapNotNull {
            it.jsonObject["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/")
        }.joinToString(", ")
    }

}
