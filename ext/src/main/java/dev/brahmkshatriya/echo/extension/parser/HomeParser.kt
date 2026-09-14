package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*

import kotlinx.serialization.json.*

class HomeParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val artistParser: ArtistParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseHomeFeed(obj: JsonObject): List<Shelf> {
        return try {
            val modules = obj["modules"]?.jsonObject ?: return emptyList()
            
            val shelves = mutableListOf<Shelf>()
            
            modules.keys.forEach { key ->
                val moduleMeta = modules[key]?.jsonObject ?: return@forEach
                val sectionData = obj[key]?.jsonArray ?: return@forEach
                
                val title = decodeHtml(moduleMeta["title"]?.jsonPrimitive?.content ?: "")
                if (title.isBlank()) return@forEach
                
                val subtitle = moduleMeta["subtitle"]?.jsonPrimitive?.content
                
                val items = sectionData.mapNotNull { element ->
                    parseHomeItem(element.jsonObject)
                }
                
                if (items.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = key,
                            title = title,
                            list = items,
                            subtitle = subtitle
                        )
                    )
                }
            }
            
            shelves
        } catch (e: Exception) {
            println("DEBUG: Failed to parse home feed: ${e.message}")
            emptyList()
        }
    }

    private fun parseHomeItem(obj: JsonObject): EchoMediaItem? {
        return when (obj["type"]?.jsonPrimitive?.content) {
            "song" -> trackParser.parseSongToTrack(obj)
            "album" -> albumParser.parseAlbumToAlbum(obj)
            "playlist" -> playlistParser.parsePlaylistToPlaylist(obj)
            "artist" -> artistParser.parseArtistToArtist(obj)
            else -> null
        }
    }
}
