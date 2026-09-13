package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import kotlinx.serialization.json.*

class PlaylistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : PlaylistClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // One-playlist cache
    private var cachedPlaylistId: String? = null
    private var cachedPlaylist: Playlist? = null
    private var cachedTracks: List<Track>? = null

    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        if (cachedPlaylistId == playlist.id && cachedPlaylist != null) {
            return cachedPlaylist!!
        }

        val response = api.getPlaylistDetails(playlist.id)
        val jsonObject = json.parseToJsonElement(response).jsonObject
        val parsedPlaylist = parser.playlist.parsePlaylistToPlaylist(jsonObject)
            ?: throw Exception("Playlist not found")
        val tracks = parser.playlist.parsePlaylistTracks(jsonObject)

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
