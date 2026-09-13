package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*

import kotlinx.serialization.json.*

data class HomeData(
    val nowTrending: List<EchoMediaItem>,
    val topPlaylists: List<Playlist>,
    val newAlbums: List<EchoMediaItem>,
    val topCharts: List<Playlist>
)

class HomeParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseHomeData(jsonString: String): HomeData? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject

            val nowTrending = mutableListOf<EchoMediaItem>()
            val topPlaylists = mutableListOf<Playlist>()
            val newAlbums = mutableListOf<EchoMediaItem>()
            val topCharts = mutableListOf<Playlist>()

            // Now Trending
            jsonObject["new_trending"]?.jsonArray?.forEach { item ->
                try {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "song" -> trackParser.parseSongToTrack(obj)?.let { nowTrending.add(it) }
                        "album" -> albumParser.parseAlbumToAlbum(obj)?.let { nowTrending.add(it) }
                        "playlist" -> playlistParser.parsePlaylistToPlaylist(obj)?.let { nowTrending.add(it) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse trending item: ${e.message}")
                }
            }

            // Top Playlists
            jsonObject["top_playlists"]?.jsonArray?.forEach { item ->
                try {
                    playlistParser.parsePlaylistToPlaylist(item.jsonObject)?.let { topPlaylists.add(it) }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse top playlist: ${e.message}")
                }
            }

            // New Albums
            jsonObject["new_albums"]?.jsonArray?.forEach { item ->
                try {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "album" -> albumParser.parseAlbumToAlbum(obj)?.let { newAlbums.add(it) }
                        "song" -> trackParser.parseSongToTrack(obj)?.let { newAlbums.add(it) }
                        "playlist" -> playlistParser.parsePlaylistToPlaylist(obj)?.let { newAlbums.add(it) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse new album: ${e.message}")
                }
            }

            // Top Charts
            jsonObject["charts"]?.jsonArray?.forEach { item ->
                try {
                    playlistParser.parsePlaylistToPlaylist(item.jsonObject)?.let { topCharts.add(it) }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse chart: ${e.message}")
                }
            }

            HomeData(
                nowTrending = nowTrending,
                topPlaylists = topPlaylists,
                newAlbums = newAlbums,
                topCharts = topCharts
            )
        } catch (e: Exception) {
            println("ERROR parsing home data: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
