package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser

class PlaylistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : PlaylistClient {

    // One-playlist cache
    private var cachedPlaylistId: String? = null
    private var cachedPlaylist: Playlist? = null
    private var cachedTracks: List<Track>? = null

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        if (cachedPlaylistId == playlist.id && cachedPlaylist != null) {
            return cachedPlaylist!!
        }

        val response = api.playlist.getDetails(playlist.id)
        val parsedPlaylist = parser.playlist.parsePlaylistToPlaylist(response)
            ?: throw Exception("Playlist not found")
        val tracks = parser.playlist.parsePlaylistTracks(response)

        cachedPlaylistId = playlist.id
        cachedPlaylist = parsedPlaylist
        cachedTracks = tracks

        return parsedPlaylist
    }

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        if (cachedPlaylistId == playlist.id && cachedTracks != null) {
            return cachedTracks!!.toFeed() as Feed<Track>
        }
        return emptyList<Track>().toFeed() as Feed<Track>
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? = null
}
