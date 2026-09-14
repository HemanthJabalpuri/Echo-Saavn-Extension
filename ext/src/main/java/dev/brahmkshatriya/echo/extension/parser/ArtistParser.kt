package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import kotlinx.serialization.json.*

class ArtistParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseArtistToArtist(obj: JsonObject): Artist? {
        // Try detail format first, fallback to search format
        val permaUrl = obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content
            ?: obj["perma_url"]?.jsonPrimitive?.content
            ?: ""
        val numericId = obj["id"]?.jsonPrimitive?.content ?: ""
        val id = if (permaUrl.isNotBlank()) permaUrl.substringAfterLast("/") else numericId
        if (id.isBlank()) return null

        return Artist(
            id = id,
            name = decodeHtml(obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: ""),
            cover = convertImageUrl(obj["image"]?.jsonPrimitive?.content).toImageHolder(),
            bio = parseBio(obj["bio"]?.jsonPrimitive?.content),
            subtitle = obj["subtitle"]?.jsonPrimitive?.content,
            extras = mapOf(
                "permaUrl" to permaUrl,
                "artistId" to numericId
            )
        )
    }

    private fun parseBio(bioJsonString: String?): String? {
        if (bioJsonString.isNullOrBlank()) return null
        
        return try {
            val jsonArray = json.parseToJsonElement(bioJsonString).jsonArray
            jsonArray
                .sortedBy { it.jsonObject["sequence"]?.jsonPrimitive?.intOrNull ?: 0 }
                .mapIndexedNotNull { index, element ->
                    val obj = element.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content
                    val text = obj["text"]?.jsonPrimitive?.content
                    if (text.isNullOrBlank()) return@mapIndexedNotNull null
                    
                    val cleaned = cleanBioText(text)
                    
                    if (index == 0 || title.isNullOrBlank()) cleaned
                    else "$title\n$cleaned"
                }
                .joinToString("\n\n")
                .ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanBioText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            // Fix mojibake (UTF-8 bytes interpreted as Latin-1)
            .replace("\u00e2\u20ac\u02dc", "'")   // '
            .replace("\u00e2\u20ac\u2122", "'")   // '
            .replace("\u00e2\u20ac\u0153", "\"")  // "
            .replace("\u00e2\u20ac\u009d", "\"")  // "
            .replace("\u00e2\u20ac\u201c", "—")   // —
            .replace("\u00e2\u20ac\u201d", "—")   // —
            .replace("\u00e2\u20ac\u00a6", "…")   // …
            .replace("\u00e2\u20ac\u00a2", "•")   // bullet point
            .replace("\u00c2\u00a0", " ")         // non-breaking space
            .replace("\u00c3\u00a9", "é")
            .replace("\u00c3\u00a8", "è")
            .replace("\u00c3\u00a0", "à")
            .replace("\u00c3\u00b1", "ñ")
            .replace("\u00c3\u00b4", "ô")
            .replace("\u00c3\u00a7", "ç")
            .trim()
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

    fun parseArtistSingles(obj: JsonObject): List<Album> {
        return obj["singles"]?.jsonArray?.mapNotNull {
            albumParser.parseAlbumToAlbum(it.jsonObject)
        } ?: emptyList()
    }

    fun parseArtistDedicatedPlaylists(obj: JsonObject): List<Playlist> {
        return obj["dedicated_artist_playlist"]?.jsonArray?.mapNotNull {
            playlistParser.parsePlaylistToPlaylist(it.jsonObject)
        } ?: emptyList()
    }

    fun parseArtistFeaturedPlaylists(obj: JsonObject): List<Playlist> {
        return obj["featured_artist_playlist"]?.jsonArray?.mapNotNull {
            playlistParser.parsePlaylistToPlaylist(it.jsonObject)
        } ?: emptyList()
    }

    // ===== SEARCH RESULTS =====
    fun parseArtistSearchResults(obj: JsonObject): List<Artist> {
        val results = obj["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { parseArtistToArtist(it.jsonObject) }
    }

    // ===== DETAILS =====
    fun parseArtistDetails(obj: JsonObject): Artist? {
        return try {
            parseArtistToArtist(obj)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse artist details: ${e.message}")
            null
        }
    }
}
