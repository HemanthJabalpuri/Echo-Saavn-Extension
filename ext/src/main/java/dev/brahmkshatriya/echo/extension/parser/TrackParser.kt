package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class TrackParser : BaseParser() {


    fun parseSongSearchResults(jsonString: String): List<SongResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseSongResults(results)
    }

    fun parseSongDetails(jsonString: String): List<SongDetail> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val songsArray = jsonObject["songs"]?.jsonArray ?: return emptyList()
            songsArray.mapNotNull { element ->
                parseSongDetail(element.jsonObject)
            }
        } catch (e: Exception) {
            println("DEBUG: Failed to parse song details: ${e.message}")
            emptyList()
        }
    }
    
    fun parseSongFromJson(obj: JsonObject): SongResult? {
        return try {
            SongResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                numericId = obj["id"]?.jsonPrimitive?.content ?: "",  // Original numeric ID
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content 
                    ?: obj["song"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                album = decodeHtml(obj["album"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("album")?.jsonPrimitive?.content ?: ""),
                albumId = obj["albumid"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("albumid")?.jsonPrimitive?.content,
                duration = obj["duration"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("duration")?.jsonPrimitive?.content ?: "0",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                primaryArtists = decodeHtml(obj["primary_artists"]?.jsonPrimitive?.content 
                    ?: obj["music"]?.jsonPrimitive?.content ?: ""),
                year = obj["year"]?.jsonPrimitive?.content ?: "",
                playCount = obj["play_count"]?.jsonPrimitive?.content ?: "0",
                type = obj["type"]?.jsonPrimitive?.content ?: "song",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    internal fun parseSongResults(array: JsonArray?): List<SongResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                SongResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    numericId = obj["id"]?.jsonPrimitive?.content ?: "",  // Original numeric ID
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "song",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    year = obj["year"]?.jsonPrimitive?.content ?: "",
                    playCount = obj["play_count"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    primaryArtists = obj["more_info"]?.jsonObject?.get("artistMap")?.jsonObject
                        ?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: "",
                    albumId = obj["more_info"]?.jsonObject?.get("album_id")?.jsonPrimitive?.content,
                    album = obj["more_info"]?.jsonObject?.get("album")?.jsonPrimitive?.content ?: "",
                    duration = obj["more_info"]?.jsonObject?.get("duration")?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    internal fun parseSongDetail(obj: JsonObject): SongDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
        val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""

        val featuredArtists = artistMap?.get("featured_artists")?.jsonArray
            ?.let { extractArtistNames(it) }
            ?.ifEmpty { null }

        val encryptedMediaUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
        val streamUrls = encryptedMediaUrl?.let { decryptUrl(it) }

        return SongDetail(
            id = id,
            numericId = obj["id"]?.jsonPrimitive?.content ?: "",  // Original numeric ID
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = "song",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
            playCount = obj["play_count"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            primaryArtists = primaryArtists,
            primaryArtistsId = primaryArtistsId,
            featuredArtists = featuredArtists,
            albumId = moreInfo?.get("album_url")?.jsonPrimitive?.content?.substringAfterLast("/"),
            album = decodeHtml(moreInfo?.get("album")?.jsonPrimitive?.content ?: ""),
            albumUrl = moreInfo?.get("album_url")?.jsonPrimitive?.content,
            duration = moreInfo?.get("duration")?.jsonPrimitive?.content ?: "0",
            label = moreInfo?.get("label")?.jsonPrimitive?.content ?: "",
            copyright = moreInfo?.get("copyright_text")?.jsonPrimitive?.content ?: "",
            releaseDate = moreInfo?.get("release_date")?.jsonPrimitive?.content,
            hasLyrics = moreInfo?.get("has_lyrics")?.jsonPrimitive?.content == "true",
            lyricsId = null,
            encryptedMediaUrl = encryptedMediaUrl,
            streamUrls = streamUrls,
            is320kbps = moreInfo?.get("320kbps")?.jsonPrimitive?.content == "true"
        )
    }

}
