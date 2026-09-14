package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Streamable
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.convertImageUrl
import dev.brahmkshatriya.echo.extension.utils.parseDate
import dev.brahmkshatriya.echo.extension.utils.parseDuration

class TrackParser : BaseParser() {

    fun parseSongToTrack(obj: JsonObject): Track? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val numericId = obj["id"]?.jsonPrimitive?.content ?: ""
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        // Artists
        val artists = parseArtistsFromArtistMap(artistMap)

        // Album
        val albumUrl = moreInfo?.get("album_url")?.jsonPrimitive?.content
        val albumId = albumUrl?.substringAfterLast("/")
        val albumTitle = moreInfo?.get("album")?.jsonPrimitive?.content ?: ""
        val image = obj["image"]?.jsonPrimitive?.content ?: ""
        val album = albumId?.let {
            Album(
                id = it,
                title = decodeHtml(albumTitle),
                cover = convertImageUrl(image).toImageHolder(),
                label = moreInfo?.get("label")?.jsonPrimitive?.content,
                isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                extras = mapOf(
                    "permaUrl" to (albumUrl ?: "")
                )
            )
        }

        // Streamables
        val encryptedMediaUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
        val streamables = if (encryptedMediaUrl != null) {
            listOf(
                Streamable.server(
                    id = "server_$id",
                    quality = 320,
                    title = "Audio Stream",
                    extras = mapOf(
                        "encryptedMediaUrl" to encryptedMediaUrl,
                        "trackId" to id
                    )
                )
            )
        } else emptyList()

        return Track(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            cover = convertImageUrl(image).toImageHolder(),
            artists = artists,
            album = album,
            duration = parseDuration(moreInfo?.get("duration")?.jsonPrimitive?.content ?: "0"),
            plays = obj["play_count"]?.jsonPrimitive?.content?.toLongOrNull(),
            releaseDate = parseDate(moreInfo?.get("release_date")?.jsonPrimitive?.content),
            genres = listOf(obj["language"]?.jsonPrimitive?.content ?: "").filter { it.isNotBlank() },
            isExplicit = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            extras = mapOf(
                "songId" to numericId,
                "permaUrl" to (obj["perma_url"]?.jsonPrimitive?.content ?: ""),
                "hasLyrics" to (moreInfo?.get("has_lyrics")?.jsonPrimitive?.content ?: "false"),
            ),
            streamables = streamables
        )
    }

    fun parseSongSearchResults(obj: JsonObject): List<Track> {
        return try {
            val results = obj["results"]?.jsonArray ?: return emptyList()
            results.mapNotNull { parseSongToTrack(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSongDetails(obj: JsonObject): List<Track> {
        return try {
            val songsArray = obj["songs"]?.jsonArray ?: return emptyList()
            songsArray.mapNotNull { parseSongToTrack(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
