package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.Track

import kotlinx.serialization.json.*

class RadioParser(
    private val trackParser: TrackParser
) : BaseParser() {

    fun parseStationId(obj: JsonObject): String? {
        return try {
            val stationId = obj["stationid"]?.jsonPrimitive?.content
            println("DEBUG: Extracted station ID: $stationId")
            stationId
        } catch (e: Exception) {
            println("DEBUG: Failed to parse station ID: ${e.message}")
            null
        }
    }

    fun parseSongSuggestions(obj: JsonObject): List<Track> {
        return try {            
            val songs = mutableListOf<Track>()
            
            obj.forEach { (key, value) ->
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
