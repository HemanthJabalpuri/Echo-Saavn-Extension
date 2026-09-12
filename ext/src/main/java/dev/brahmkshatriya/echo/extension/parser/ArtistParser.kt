package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class ArtistParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser
) : BaseParser() {

    fun parseArtistSearchResults(jsonString: String): List<ArtistResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseArtistResults(results)
    }

    fun parseArtistDetails(jsonString: String): ArtistDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseArtistDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse artist details: ${e.message}")
            null
        }
    }

    internal fun parseArtistResults(array: JsonArray?): List<ArtistResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                ArtistResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    name = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "artist",
                    role = obj["description"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    internal fun parseArtistDetail(obj: JsonObject): ArtistDetail? {
        val id = obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null

        val topSongs = obj["topSongs"]?.jsonArray?.mapNotNull { trackParser.parseSongToTrack(it.jsonObject) } ?: emptyList()
        val topAlbums = obj["topAlbums"]?.jsonArray?.mapNotNull { albumParser.parseAlbumResults(buildJsonArray { add(it) })?.firstOrNull() } ?: emptyList()
        
        // Extract isRadioPresent (top-level boolean)
        val isRadioPresent = obj["isRadioPresent"]?.jsonPrimitive?.booleanOrNull ?: false

        return ArtistDetail(
            id = id,
            name = decodeHtml(obj["name"]?.jsonPrimitive?.content ?: ""),
            subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            followerCount = obj["follower_count"]?.jsonPrimitive?.content ?: "0",
            type = obj["type"]?.jsonPrimitive?.content ?: "artist",
            isVerified = obj["isVerified"]?.jsonPrimitive?.booleanOrNull ?: false,
            dominantLanguage = obj["dominantLanguage"]?.jsonPrimitive?.content ?: "",
            dominantType = obj["dominantType"]?.jsonPrimitive?.content ?: "",
            topSongs = topSongs,
            topAlbums = topAlbums,
            isRadioPresent = isRadioPresent
        )
    }


}
