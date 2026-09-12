package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.utils.*
import kotlinx.serialization.json.*

class TrackParser : BaseParser() {

    // ===== SINGLE PARSER (used everywhere) =====
    fun parseSongToTrack(obj: JsonObject): Track? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val numericId = obj["id"]?.jsonPrimitive?.content ?: ""
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

        // Album
        val albumId = moreInfo?.get("album_url")?.jsonPrimitive?.content?.substringAfterLast("/")
        val albumTitle = moreInfo?.get("album")?.jsonPrimitive?.content ?: ""
        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val album = albumId?.let {
            Album(
                id = it,
                title = decodeHtml(albumTitle),
                type = null,
                cover = convertImageUrl(image).toImageHolder(),
                artists = emptyList(),
                trackCount = null,
                duration = null,
                releaseDate = null,
                description = null,
                background = null,
                label = moreInfo?.get("label")?.jsonPrimitive?.content,
                isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                subtitle = null,
                extras = emptyMap()
            )
        }

        // Streamables
        val encryptedMediaUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
        val streamUrls = encryptedMediaUrl?.let { decryptUrl(it) }
        val streamables = if (streamUrls != null) {
            val urlsString = "low=${streamUrls.low}, medium=${streamUrls.medium}, high=${streamUrls.high}, veryHigh=${streamUrls.veryHigh}"
            listOf(
                Streamable.server(
                    id = "server_$id",
                    quality = 320,
                    title = "Audio Stream",
                    extras = mapOf(
                        "streamUrls" to urlsString,
                        "trackId" to id
                    )
                )
            )
        } else emptyList()

        return Track(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            type = Track.Type.Song,
            cover = convertImageUrl(image).toImageHolder(),
            artists = artists,
            album = album,
            duration = parseDuration(moreInfo?.get("duration")?.jsonPrimitive?.content ?: "0"),
            playedDuration = null,
            plays = obj["play_count"]?.jsonPrimitive?.content?.toLongOrNull(),
            releaseDate = parseDate(moreInfo?.get("release_date")?.jsonPrimitive?.content),
            description = null,
            background = convertImageUrl(image).toImageHolder(),
            genres = listOf(obj["language"]?.jsonPrimitive?.content ?: "").filter { it.isNotBlank() },
            isrc = null,
            albumOrderNumber = null,
            albumDiscNumber = null,
            playlistAddedDate = null,
            isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "songId" to numericId,
                "language" to (obj["language"]?.jsonPrimitive?.content ?: ""),
                "year" to (obj["year"]?.jsonPrimitive?.content ?: ""),
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
                "hasLyrics" to (moreInfo?.get("has_lyrics")?.jsonPrimitive?.content ?: "false"),
                "label" to (moreInfo?.get("label")?.jsonPrimitive?.content ?: ""),
                "copyright" to (moreInfo?.get("copyright_text")?.jsonPrimitive?.content ?: "")
            ),
            isPlayable = Track.Playable.Yes,
            streamables = streamables
        )
    }

    // ===== LIST PARSERS =====
    fun parseSongSearchResults(jsonString: String): List<Track> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val results = jsonObject["results"]?.jsonArray ?: return emptyList()
            results.mapNotNull { parseSongToTrack(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSongDetails(jsonString: String): List<Track> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val songsArray = jsonObject["songs"]?.jsonArray ?: return emptyList()
            songsArray.mapNotNull { parseSongToTrack(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSongFromJson(obj: JsonObject): Track? = parseSongToTrack(obj)
}
