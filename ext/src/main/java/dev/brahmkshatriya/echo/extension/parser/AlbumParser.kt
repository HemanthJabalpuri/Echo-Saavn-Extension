package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class AlbumParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parseAlbumSearchResults(jsonString: String): List<AlbumResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseAlbumResults(results)
    }
    
    fun parseAlbumDetails(jsonString: String): AlbumDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseAlbumDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse album details: ${e.message}")
            null
        }
    }
    
    fun parseAlbumFromJson(obj: JsonObject): AlbumResult? {
        return try {
            AlbumResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content 
                    ?: obj["header_desc"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                year = obj["year"]?.jsonPrimitive?.content ?: "",
                songCount = obj["song_count"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("song_count")?.jsonPrimitive?.content ?: "0",
                type = obj["type"]?.jsonPrimitive?.content ?: "",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    internal fun parseAlbumResults(array: JsonArray?): List<AlbumResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val moreInfo = obj["more_info"]?.jsonObject
                val artistMap = moreInfo?.get("artistMap")?.jsonObject
                
                val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
                val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""
                
                AlbumResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "album",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    year = obj["year"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content ?: "0",
                    primaryArtists = primaryArtists,
                    primaryArtistsId = primaryArtistsId
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    internal fun parseAlbumDetail(obj: JsonObject): AlbumDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
        val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""

        val songs = obj["list"]?.jsonArray?.mapNotNull { trackParser.parseSongDetail(it.jsonObject) } ?: emptyList()
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content ?: "0"

        return AlbumDetail(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = obj["type"]?.jsonPrimitive?.content ?: "album",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            primaryArtists = primaryArtists,
            primaryArtistsId = primaryArtistsId,
            songCount = songCount,
            releaseDate = obj["year"]?.jsonPrimitive?.content, // year as release date
            songs = songs
        )
    }

}
