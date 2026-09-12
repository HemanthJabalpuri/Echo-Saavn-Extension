package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.*

class AlbumClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : AlbumClient {

    override suspend fun loadAlbum(album: Album): Album {
        return try {
            println("DEBUG: Loading album with ID: ${album.id}")
            val response = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetails(response)
                ?: throw Exception("Album not found")
            
            albumDetailToAlbum(albumDetail)
        } catch (e: Exception) {
            println("DEBUG: Failed to load album ${album.id}: ${e.message}")
            throw Exception("Failed to load album: ${e.message}")
        }
    }

    override suspend fun loadTracks(album: Album): Feed<Track>? {
        return try {
            println("DEBUG: Loading tracks for album: ${album.id}")
            val response = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetails(response)
                ?: return null
            
            val tracks = albumDetail.songs
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load album tracks: ${e.message}")
            null
        }
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        return null
    }
}
