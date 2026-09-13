package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import dev.brahmkshatriya.echo.extension.utils.parseDate

class AlbumParser(
    private val trackParser: TrackParser
) : BaseParser() {

    // ===== SINGLE ALBUM PARSER =====
    fun parseAlbumToAlbum(obj: JsonObject): Album? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        // Artists
        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
        val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""
        val artistNames = primaryArtists.split(", ").filter { it.isNotBlank() }
        val artistIds = primaryArtistsId.split(", ").filter { it.isNotBlank() }
        val artists = artistNames.mapIndexed { index, name ->
            Artist(
                id = if (index < artistIds.size) artistIds[index] else "",
                name = name,
                cover = null,
                bio = null,
                background = null,
                banners = emptyList(),
                subtitle = null,
                extras = emptyMap()
            )
        }

        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content
            ?: obj["song_count"]?.jsonPrimitive?.content
            ?: "0"

        return Album(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            type = null,  // Could map JioSaavn type to Album.Type if needed
            cover = convertImageUrl(image).toImageHolder(),
            artists = artists,
            trackCount = songCount.toLongOrNull(),
            duration = null,
            releaseDate = parseDate(obj["year"]?.jsonPrimitive?.content),
            description = null,
            background = convertImageUrl(image).toImageHolder(),
            label = moreInfo?.get("label")?.jsonPrimitive?.content,
            isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "language" to (obj["language"]?.jsonPrimitive?.content ?: ""),
                "year" to (obj["year"]?.jsonPrimitive?.content ?: ""),
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
                "type" to (obj["type"]?.jsonPrimitive?.content ?: "")
            )
        )
    }

    fun parseAlbumTracksFromJson(jsonString: String): List<Track> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseAlbumTracks(jsonObject)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== ALBUM TRACKS =====
    fun parseAlbumTracks(obj: JsonObject): List<Track> {
        return obj["list"]?.jsonArray?.mapNotNull {
            trackParser.parseSongToTrack(it.jsonObject)
        } ?: emptyList()
    }

    // ===== SEARCH RESULTS =====
    fun parseAlbumSearchResults(jsonString: String): List<Album> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parseAlbumToAlbum(it.jsonObject) }
    }

    // ===== DETAILS =====
    fun parseAlbumDetails(jsonString: String): Album? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseAlbumToAlbum(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse album details: ${e.message}")
            null
        }
    }
}
