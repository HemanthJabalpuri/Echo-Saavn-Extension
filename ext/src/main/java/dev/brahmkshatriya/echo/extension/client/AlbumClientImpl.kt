package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser

class AlbumClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : AlbumClient {

    private var cachedAlbumId: String? = null
    private var cachedTracks: List<Track>? = null

    // ===== LOAD ALBUM =====
    // No API call - return as-is
    override suspend fun loadAlbum(album: Album): Album {
        return album
    }

    // ===== LOAD TRACKS =====
    override suspend fun loadTracks(album: Album): Feed<Track>? {
        // Cache hit
        if (cachedAlbumId == album.id && cachedTracks != null) {
            return cachedTracks!!.toFeed() as Feed<Track>
        }

        // Fetch and parse
        val response = api.album.getDetails(album.id)
        val tracks = parser.album.parseAlbumTracks(response)

        cachedAlbumId = album.id
        cachedTracks = tracks

        return tracks.toFeed() as Feed<Track>
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? = null
}
