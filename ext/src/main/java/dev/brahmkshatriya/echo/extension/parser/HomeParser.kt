package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class HomeParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseHomeData(jsonString: String): HomeData? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject

            val nowTrending = mutableListOf<MediaItem>()
            val topPlaylists = mutableListOf<PlaylistResult>()
            val newAlbums = mutableListOf<MediaItem>()
            val topCharts = mutableListOf<PlaylistResult>()

            // Now Trending
            jsonObject["new_trending"]?.jsonArray?.forEach { item ->
                try {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""

                    when (type) {
                        "song" -> trackParser.parseSongFromJson(obj)?.let { nowTrending.add(MediaItem.Track(it)) }
                        "album" -> albumParser.parseAlbumFromJson(obj)?.let { nowTrending.add(MediaItem.Album(it)) }
                        "playlist" -> playlistParser.parsePlaylistFromJson(obj)?.let { nowTrending.add(MediaItem.Playlist(it)) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse trending item: ${e.message}")
                }
            }

            // Top Playlists
            jsonObject["top_playlists"]?.jsonArray?.forEach { item ->
                try {
                    playlistParser.parsePlaylistFromJson(item.jsonObject)?.let { topPlaylists.add(it) }
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
                        "album" -> albumParser.parseAlbumFromJson(obj)?.let { newAlbums.add(MediaItem.Album(it)) }
                        "song" -> trackParser.parseSongFromJson(obj)?.let { newAlbums.add(MediaItem.Track(it)) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse new album: ${e.message}")
                }
            }

            // Top Charts
            jsonObject["charts"]?.jsonArray?.forEach { item ->
                try {
                    playlistParser.parsePlaylistFromJson(item.jsonObject)?.let { topCharts.add(it) }
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
