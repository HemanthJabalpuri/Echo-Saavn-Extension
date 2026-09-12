package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import kotlinx.serialization.json.*

class ArtistParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser
) : BaseParser() {

    // ===== SINGLE ARTIST PARSER =====
    fun parseArtistToArtist(obj: JsonObject): Artist? {
        val id = obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content?.substringAfterLast("/")
            ?: return null

        val name = decodeHtml(obj["name"]?.jsonPrimitive?.content ?: "")
        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val followerCount = obj["follower_count"]?.jsonPrimitive?.content ?: "0"
        val type = obj["type"]?.jsonPrimitive?.content ?: "artist"
        val isVerified = obj["isVerified"]?.jsonPrimitive?.booleanOrNull ?: false
        val dominantLanguage = obj["dominantLanguage"]?.jsonPrimitive?.content ?: ""
        val dominantType = obj["dominantType"]?.jsonPrimitive?.content ?: ""
        val subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: ""

        return Artist(
            id = id,
            name = name,
            cover = convertImageUrl(image).toImageHolder(),
            bio = null,
            background = convertImageUrl(image).toImageHolder(),
            banners = emptyList(),
            subtitle = subtitle,
            extras = mapOf(
                "followerCount" to followerCount,
                "type" to type,
                "isVerified" to isVerified.toString(),
                "dominantLanguage" to dominantLanguage,
                "dominantType" to dominantType,
                "permaUrl" to (obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content ?: "")
            )
        )
    }

    // ===== TOP SONGS =====
    fun parseArtistTopSongs(obj: JsonObject): List<Track> {
        return obj["topSongs"]?.jsonArray?.mapNotNull {
            trackParser.parseSongToTrack(it.jsonObject)
        } ?: emptyList()
    }

    // ===== TOP ALBUMS =====
    fun parseArtistTopAlbums(obj: JsonObject): List<Album> {
        return obj["topAlbums"]?.jsonArray?.mapNotNull {
            albumParser.parseAlbumToAlbum(it.jsonObject)
        } ?: emptyList()
    }

    // ===== IS RADIO PRESENT =====
    fun parseArtistIsRadioPresent(obj: JsonObject): Boolean {
        return obj["isRadioPresent"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    // ===== SEARCH RESULTS =====
    fun parseArtistSearchResults(jsonString: String): List<Artist> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parseArtistToArtist(it.jsonObject) }
    }

    // ===== DETAILS =====
    fun parseArtistDetails(jsonString: String): Artist? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseArtistToArtist(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse artist details: ${e.message}")
            null
        }
    }
}
