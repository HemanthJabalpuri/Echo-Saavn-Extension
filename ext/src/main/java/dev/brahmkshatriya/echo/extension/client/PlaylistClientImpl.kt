package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.PlaylistClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.utils.Logger

class PlaylistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : PlaylistClient {

    private var cachedPlaylistId: String? = null
    private var cachedResponse: JsonObject? = null
    private var cachedPlaylist: Playlist? = null
    private var cachedTracks: List<Track>? = null

    // ===== LOAD PLAYLIST =====
    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        // Cache hit
        if (cachedPlaylistId == playlist.id && cachedPlaylist != null) {
            return cachedPlaylist!!
        }

        // Fetch, parse, cache
        val response = api.playlist.getDetails(playlist.id)
        val parsedPlaylist = parser.playlist.parsePlaylistToPlaylist(response)
            ?: throw Exception("Playlist not found")
        val tracks = parser.playlist.parsePlaylistTracks(response)

        cachedPlaylistId = playlist.id
        cachedResponse = response
        cachedPlaylist = parsedPlaylist
        cachedTracks = tracks

        return parsedPlaylist
    }

    // ===== LOAD TRACKS =====
    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        if (cachedPlaylistId == playlist.id && cachedTracks != null) {
            return cachedTracks!!.toFeed() as Feed<Track>
        }
        return emptyList<Track>().toFeed() as Feed<Track>
    }

    // ===== LOAD FEED =====
    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        val response = cachedResponse.takeIf { cachedPlaylistId == playlist.id }
            ?: return null

        val shelves = mutableListOf<Shelf>()

        // ===== RELATED PLAYLISTS =====
        val rawListId = response["id"]?.jsonPrimitive?.content
        if (!rawListId.isNullOrBlank()) {
            try {
                val recoResponse = api.playlist.getPlaylistReco(rawListId)
                val relatedPlaylists = parser.playlist.parseRelatedPlaylists(recoResponse)
                    .filter { it.id != playlist.id }

                if (relatedPlaylists.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "related_playlists",
                            title = "Related Playlists",
                            list = relatedPlaylists,
                            subtitle = "${relatedPlaylists.size} playlists"
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("PlaylistClient", "Failed to load related playlists", e)
            }
        }

        // ===== ARTISTS SHELF =====
        val artists = parser.playlist.parsePlaylistArtists(response)
        if (artists.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "playlist_artists",
                    title = "Artists",
                    list = artists,
                    subtitle = "${artists.size} artists"
                )
            )
        }

        return if (shelves.isEmpty()) null else shelves.toFeed()
    }

}
