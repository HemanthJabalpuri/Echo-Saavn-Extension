package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Shelf

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.utils.Logger

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
            Logger.e("ArtistParser", "Failed to parse home feed: ${e.message}", e)
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
