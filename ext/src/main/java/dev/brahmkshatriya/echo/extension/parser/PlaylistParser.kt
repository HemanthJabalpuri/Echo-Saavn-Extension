package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class PlaylistParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parsePlaylistSearchResults(jsonString: String): List<PlaylistResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parsePlaylistResults(results)
    }

    fun parsePlaylistDetails(jsonString: String): PlaylistDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parsePlaylistDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse playlist details: ${e.message}")
            null
        }
    }

    fun parsePlaylistFromJson(obj: JsonObject): PlaylistResult? {
        return try {
            PlaylistResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content 
                    ?: obj["listname"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content 
                    ?: obj["header_desc"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                songCount = obj["song_count"]?.jsonPrimitive?.content 
                    ?: obj["list_count"]?.jsonPrimitive?.content ?: "0",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                type = obj["type"]?.jsonPrimitive?.content ?: "",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    internal fun parsePlaylistResults(array: JsonArray?): List<PlaylistResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                PlaylistResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "playlist",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    songCount = obj["more_info"]?.jsonObject?.get("song_count")?.jsonPrimitive?.content ?: "0"
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    internal fun parsePlaylistDetail(obj: JsonObject): PlaylistDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject

        val songs = obj["list"]?.jsonArray?.mapNotNull { trackParser.parseSongToTrack(it.jsonObject) } ?: emptyList()
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content
            ?: obj["list_count"]?.jsonPrimitive?.content
            ?: "0"
        val followerCount = moreInfo?.get("follower_count")?.jsonPrimitive?.content
            ?: obj["follower_count"]?.jsonPrimitive?.content
            ?: "0"

        return PlaylistDetail(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: obj["listname"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = obj["type"]?.jsonPrimitive?.content ?: "playlist",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            songCount = songCount,
            followerCount = followerCount,
            songs = songs
        )
    }

}

