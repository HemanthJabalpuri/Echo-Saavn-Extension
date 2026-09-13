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
}
