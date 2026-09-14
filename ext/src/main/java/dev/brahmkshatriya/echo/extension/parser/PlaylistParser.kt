package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import dev.brahmkshatriya.echo.extension.utils.Logger

class PlaylistParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parsePlaylistToPlaylist(obj: JsonObject): Playlist? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null

        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val songCount = obj["list_count"]?.jsonPrimitive?.content ?: "0"

        return Playlist(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            isEditable = false,
            isPrivate = false,
            cover = convertImageUrl(image).toImageHolder(),
            trackCount = songCount.toLongOrNull(),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
            )
        )
    }

    fun parsePlaylistTracks(obj: JsonObject): List<Track> {
        return obj["list"]?.jsonArray?.mapNotNull {
            trackParser.parseSongToTrack(it.jsonObject)
        } ?: emptyList()
    }

    fun parsePlaylistSearchResults(obj: JsonObject): List<Playlist> {
        val results = obj["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parsePlaylistToPlaylist(it.jsonObject) }
    }

    fun parsePlaylistDetails(obj: JsonObject): Playlist? {
        return try {
            parsePlaylistToPlaylist(obj)
        } catch (e: Exception) {
            Logger.e("PlaylistParser", "Failed to parse playlist details: ${e.message}", e)
            null
        }
    }
}
