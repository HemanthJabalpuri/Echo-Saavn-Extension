package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Track

import kotlinx.serialization.json.*

class RadioParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parseStationId(jsonString: String): String? {
        return try {
            println("DEBUG: Parsing station ID from response (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            val stationId = jsonElement.jsonObject["stationid"]?.jsonPrimitive?.content
            println("DEBUG: Extracted station ID: $stationId")
            stationId
        } catch (e: Exception) {
            println("DEBUG: Failed to parse station ID: ${e.message}")
            null
        }
    }
    
    fun parseSongSuggestions(jsonString: String): List<Track> {
        return try {
            println("DEBUG: Parsing song suggestions (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject
            
            val songs = mutableListOf<Track>()
            
            jsonObject.forEach { (key, value) ->
                if (key != "stationid") {
                    try {
                        val songObject = value.jsonObject["song"]?.jsonObject
                        if (songObject != null) {
                            val song = trackParser.parseSongToTrack(songObject)
                            song?.let { songs.add(it) }
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to parse song at key $key: ${e.message}")
                    }
                }
            }
            
            println("DEBUG: Successfully parsed ${songs.size} song suggestions from station")
            songs
        } catch (e: Exception) {
            println("DEBUG: Failed to parse song suggestions: ${e.message}")
            println("DEBUG: Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun parseSimilarSongs(jsonString: String): List<Track> {
        return try {
            println("DEBUG: Similar songs raw response (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            println("DEBUG: JSON element type: ${jsonElement::class.simpleName}")
            
            val jsonArray = jsonElement.jsonArray
            println("DEBUG: Similar songs array size: ${jsonArray.size}")
            
            val results = jsonArray.mapNotNull { trackParser.parseSongToTrack(it.jsonObject) }
            println("DEBUG: Parsed ${results.size} similar songs")
            results
        } catch (e: Exception) {
            println("DEBUG: Failed to parse similar songs: ${e.message}")
            println("DEBUG: Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun parseRadioSongs(response: String): List<Track> {
        return try {
            val jsonObject = json.parseToJsonElement(response).jsonObject
            val songs = mutableListOf<Track>()
            
            jsonObject.keys.forEach { key ->
                try {
                    // Only proceed if the value is a JsonObject
                    val value = jsonObject[key]
                    if (value is JsonObject) {
                        val songObject = value["song"] as? JsonObject
                        if (songObject != null) {
                            trackParser.parseSongToTrack(songObject)?.let { songs.add(it) }
                        }
                    }
                } catch (e: Exception) {
                    // Skip this key and continue
                }
            }
            
            songs
        } catch (e: Exception) {
            println("DEBUGG: Failed to parse radio songs: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseArtistRadioStationId(response: String): String? {
        return try {
            val jsonObject = json.parseToJsonElement(response).jsonObject
            jsonObject["stationid"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            println("DEBUG: Failed to parse station ID: ${e.message}")
            null
        }
    }

}
