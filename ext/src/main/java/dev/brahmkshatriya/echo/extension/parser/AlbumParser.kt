package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import dev.brahmkshatriya.echo.extension.utils.parseDate

class AlbumParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parseAlbumToAlbum(obj: JsonObject): Album? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        val artists = parseArtistsFromArtistMap(artistMap)

        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content ?: "0"

        return Album(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            cover = convertImageUrl(image).toImageHolder(),
            artists = artists,
            trackCount = songCount.toLongOrNull(),
            releaseDate = parseDate(obj["year"]?.jsonPrimitive?.content),
            isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
            )
        )
    }

    fun parseAlbumTracks(obj: JsonObject): List<Track> {
        return obj["list"]?.jsonArray?.mapNotNull {
            trackParser.parseSongToTrack(it.jsonObject)
        } ?: emptyList()
    }

    fun parseAlbumSearchResults(obj: JsonObject): List<Album> {
        val results = obj["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parseAlbumToAlbum(it.jsonObject) }
    }

    fun parseAlbumDetails(obj: JsonObject): Album? {
        return try {
            parseAlbumToAlbum(obj)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse album details: ${e.message}")
            null
        }
    }
}
