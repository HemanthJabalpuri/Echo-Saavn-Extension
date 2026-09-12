package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import kotlinx.serialization.json.*

class PlaylistParser(
    private val trackParser: TrackParser
) : BaseParser() {

    // ===== SINGLE PLAYLIST PARSER =====
    fun parsePlaylistToPlaylist(obj: JsonObject): Playlist? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject

        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content
            ?: obj["list_count"]?.jsonPrimitive?.content
            ?: obj["song_count"]?.jsonPrimitive?.content
            ?: "0"
        val followerCount = moreInfo?.get("follower_count")?.jsonPrimitive?.content
            ?: obj["follower_count"]?.jsonPrimitive?.content
            ?: "0"

        return Playlist(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content
                ?: obj["listname"]?.jsonPrimitive?.content ?: ""),
            isEditable = false,
            isPrivate = false,
            cover = convertImageUrl(image).toImageHolder(),
            authors = emptyList(),
            trackCount = songCount.toLongOrNull(),
            duration = null,
            creationDate = null,
            description = null,
            background = convertImageUrl(image).toImageHolder(),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "language" to (obj["language"]?.jsonPrimitive?.content ?: ""),
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
                "type" to (obj["type"]?.jsonPrimitive?.content ?: ""),
                "followerCount" to followerCount
            )
        )
    }

    // ===== PLAYLIST TRACKS =====
    fun parsePlaylistTracks(obj: JsonObject): List<Track> {
        return obj["list"]?.jsonArray?.mapNotNull {
            trackParser.parseSongToTrack(it.jsonObject)
        } ?: emptyList()
    }

    // ===== HELPER: From JSON string =====
    fun parsePlaylistTracksFromJson(jsonString: String): List<Track> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parsePlaylistTracks(jsonObject)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== SEARCH RESULTS =====
    fun parsePlaylistSearchResults(jsonString: String): List<Playlist> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parsePlaylistToPlaylist(it.jsonObject) }
    }

    // ===== DETAILS =====
    fun parsePlaylistDetails(jsonString: String): Playlist? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parsePlaylistToPlaylist(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse playlist details: ${e.message}")
            null
        }
    }
}
