package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.*

class PlaylistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : PlaylistClient {

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return try {
            println("DEBUG: Loading playlist with ID: ${playlist.id}")
            val response = api.getPlaylistDetails(playlist.id)
            println("DEBUG: Playlist response length: ${response.length}")
            println("DEBUG: Playlist response preview: ${response.take(500)}")
            
            val playlistDetail = parser.parsePlaylistDetails(response)
                ?: throw Exception("Playlist not found")
            
            println("DEBUG: Parsed playlist: ${playlistDetail.title}, songs: ${playlistDetail.songs.size}")
            playlistDetailToPlaylist(playlistDetail)
        } catch (e: Exception) {
            println("DEBUG: Failed to load playlist ${playlist.id}: ${e.message}")
            e.printStackTrace()
            throw Exception("Failed to load playlist: ${e.message}")
        }
    }
    
    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        return try {
            println("DEBUG: Loading tracks for playlist: ${playlist.id}")
            val response = api.getPlaylistDetails(playlist.id)
            val playlistDetail = parser.parsePlaylistDetails(response)
                ?: return emptyList<Track>().toFeed() as Feed<Track>
            
            println("DEBUG: Playlist has ${playlistDetail.songs.size} songs")
            
            if (playlistDetail.songs.isEmpty()) {
                println("DEBUG: Playlist songs list is empty, returning empty feed")
                return emptyList<Track>().toFeed() as Feed<Track>
            }
            
            val tracks = playlistDetail.songs.map { songDetailToTrack(it) }
            println("DEBUG: Converted ${tracks.size} songs to tracks")
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load playlist tracks: ${e.message}")
            e.printStackTrace()
            emptyList<Track>().toFeed() as Feed<Track>
        }
    }
    
    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        return null
    }

}
