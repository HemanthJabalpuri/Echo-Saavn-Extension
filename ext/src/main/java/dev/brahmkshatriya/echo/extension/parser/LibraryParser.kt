package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class LibraryParser : BaseParser() {

    fun parseSongIds(library: JsonObject): List<String> =
        library["song"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun parseAlbumIds(library: JsonObject): List<String> =
        library["album"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun parseArtistIds(library: JsonObject): List<String> =
        library["artist"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun parsePlaylistEntries(library: JsonObject): List<PlaylistEntry> =
        library["playlist"]?.jsonArray
            ?.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val ts = obj["ts"]?.jsonPrimitive?.contentOrNull
                PlaylistEntry(id = id, timestamp = ts)
            }
            ?: emptyList()

    data class PlaylistEntry(val id: String, val timestamp: String?)
}
