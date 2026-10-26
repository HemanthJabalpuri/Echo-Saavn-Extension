package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.PlaylistEditClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.helpers.ClientException

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.storage.LocalPlaylistStore
import dev.brahmkshatriya.echo.extension.storage.LocalPlaylistTracksStore
import dev.brahmkshatriya.echo.extension.storage.LocalLikedTracksStore
import dev.brahmkshatriya.echo.extension.utils.Logger
import dev.brahmkshatriya.echo.extension.utils.getToken
import dev.brahmkshatriya.echo.extension.utils.Prefixes

class PlaylistEditClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : PlaylistEditClient {

    private val settings get() = SaavnDependencies.settings

    // ===== PLAYLIST CLIENT =====

    private var cachedPlaylistId: String? = null
    private var cachedResponse: JsonObject? = null
    private var cachedPlaylist: Playlist? = null
    private var cachedTracks: List<Track>? = null

    // ===== LOAD PLAYLIST =====
    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        // Liked playlist — synthetic, read-only
        if (Prefixes.isLikedPlaylist(playlist.id)) {
            val count = settings?.let { LocalLikedTracksStore.getAll(it).size.toLong() } ?: 0L
            return playlist.copy(
                title = "Liked Songs",
                isEditable = false,
                trackCount = count,
            )
        }

        // Local playlist
        if (Prefixes.isLocal(playlist.id)) {
            return playlist.copy(isEditable = true)
        }

        // Cache hit
        if (cachedPlaylistId == playlist.id && cachedPlaylist != null) {
            return applyEditable(cachedPlaylist!!)
        }

        // Fetch, parse, cache
        val token = playlist.getToken()
        val response = api.playlist.getDetails(token)
        val parsedPlaylist = parser.playlist.parsePlaylistToPlaylist(response)
            ?: throw Exception("Playlist not found")
        val tracks = parser.playlist.parsePlaylistTracks(response)

        cachedPlaylistId = playlist.id
        cachedResponse = response
        cachedPlaylist = parsedPlaylist
        cachedTracks = tracks

        return applyEditable(parsedPlaylist)
    }

    private fun applyEditable(playlist: Playlist): Playlist {
        val uid = playlist.extras["uid"].orEmpty()
        val currentUser = SaavnDependencies.currentUserId
        val editable = currentUser != null && uid.isNotBlank() && uid == currentUser
        return playlist.copy(isEditable = editable)
    }

    // ===== LOAD TRACKS =====
    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        // Liked playlist
        if (Prefixes.isLikedPlaylist(playlist.id)) {
            val settings = settings ?: return emptyList<Track>().toFeed() as Feed<Track>
            return LocalLikedTracksStore.getAll(settings).toFeed() as Feed<Track>
        }

        // Local playlist
        if (Prefixes.isLocal(playlist.id)) {
            val settings = settings ?: return emptyList<Track>().toFeed() as Feed<Track>
            val tracks = LocalPlaylistTracksStore.getTracks(settings, playlist.id)
            return tracks.toFeed() as Feed<Track>
        }

        // Cache check
        val cached = cachedTracks.takeIf { cachedPlaylistId == playlist.id }
        val totalCount = cachedPlaylist?.trackCount?.toInt()

        // All tracks cached — return static feed
        if (cached != null && totalCount != null && cached.size >= totalCount) {
            Logger.d("PlaylistClient", "All $totalCount tracks cached — static feed")
            return cached.toFeed() as Feed<Track>
        }

        // Need pagination
        val token = playlist.getToken()
        if (token.isBlank()) return emptyList<Track>().toFeed() as Feed<Track>

        val pagedData = PagedData.Concat(
            PagedData.Single { cached ?: emptyList() },
            PagedData.Continuous { continuation ->
                val page = continuation?.toIntOrNull() ?: 2
                try {
                    val response = api.playlist.getDetails(token, page = page, count = 100)
                    val tracks = parser.playlist.parsePlaylistTracks(response)
                    val next = if (tracks.size >= 100) (page + 1).toString() else null
                    Page(tracks, next)
                } catch (e: Exception) {
                    Logger.e("PlaylistClient", "Failed to load tracks page $page", e)
                    Page(emptyList(), null)
                }
            }
        )
        return pagedData.toFeed()
    }

    // ===== LOAD FEED =====
    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        val response = cachedResponse.takeIf { cachedPlaylistId == playlist.id }
            ?: return null

        val shelves = mutableListOf<Shelf>()

        // ===== RELATED PLAYLISTS =====
        try {
            val recoResponse = api.playlist.getPlaylistReco(playlist.id)
            val relatedPlaylists = parser.playlist.parsePlaylistResults(recoResponse)
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

        // ===== TRENDING PLAYLISTS SHELF =====
        val language = response["modules"]?.jsonObject
            ?.get("currentlyTrendingPlaylists")?.jsonObject
            ?.get("source_params")?.jsonObject
            ?.get("entity_language")?.jsonPrimitive?.content

        if (!language.isNullOrBlank()) {
            try {
                val trendingResponse = api.home.getTrending("playlist", language)
                val trendingPlaylists = parser.playlist.parsePlaylistResults(trendingResponse)
                    .filter { it.id != playlist.id }

                if (trendingPlaylists.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "trending_playlists",
                            title = "Trending Playlists",
                            list = trendingPlaylists,
                            subtitle = "${trendingPlaylists.size} playlists"
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("PlaylistClient", "Failed to load trending playlists", e)
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


    // ===== PLAYLIST EDIT CLIENT =====

    override suspend fun listEditablePlaylists(track: Track?): List<Pair<Playlist, Boolean>> {
        val settings = settings ?: return emptyList()
        val result = mutableListOf<Pair<Playlist, Boolean>>()

        // ---- Local playlists ----
        val localPlaylists = LocalPlaylistStore.getAll(settings).filter { it.isEditable }
        localPlaylists.forEach { playlist ->
            val isInPlaylist = track != null &&
                    LocalPlaylistTracksStore.getTracks(settings, playlist.id)
                        .any { it.id == track.id }
            result.add(playlist to isInPlaylist)
        }

        // ---- Saavn playlists (owned by current user) ----
        val currentUser = SaavnDependencies.currentUserId
        if (currentUser != null) {
            try {
                val response = api.playlistEdit.listMyPlaylists()
                val rawArray = response["results"]?.jsonArray
                if (rawArray != null) {
                    rawArray.forEach { element ->
                        val obj = element.jsonObject
                        val parsed = parser.playlist.parsePlaylistToPlaylist(obj) ?: return@forEach
                        if (parsed.extras["uid"] != currentUser) return@forEach

                        val contentsRaw = obj["more_info"]?.jsonObject
                            ?.get("contents")?.jsonPrimitive?.contentOrNull
                            .orEmpty()
                        val isInPlaylist = track != null &&
                                contentsRaw.split(",").any { it.trim() == track.id }

                        result.add(parsed.copy(isEditable = true) to isInPlaylist)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("PlaylistEdit", "Failed to fetch Saavn playlists", e)
                // swallow — local playlists still shown
            }
        }

        Logger.d("PlaylistEdit", "listEditablePlaylists: ${result.size} total")
        return result
    }

    override suspend fun createPlaylist(title: String, description: String?): Playlist {
        val settings = settings ?: throw Exception("Settings unavailable")
        val playlist = Playlist(
            id = Prefixes.newLocalId(),
            title = title,
            isEditable = true,
            isPrivate = false,
            description = description,
            creationDate = Date(System.currentTimeMillis()),
            cover = null,
            authors = emptyList(),
            trackCount = 0,
            duration = null,
            background = null,
            subtitle = null,
            extras = emptyMap()
        )
        LocalPlaylistStore.add(settings, playlist)
        LocalPlaylistTracksStore.setTracks(settings, playlist.id, emptyList())
        Logger.d("PlaylistEdit", "createPlaylist: ${playlist.id} - $title")
        return playlist
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        if (!Prefixes.isLocal(playlist.id)) {
            throw ClientException.NotSupported("deletePlaylist")
        }
        val settings = settings ?: return
        LocalPlaylistStore.remove(settings, playlist.id)
        LocalPlaylistTracksStore.delete(settings, playlist.id)
        Logger.d("PlaylistEdit", "deletePlaylist: ${playlist.id}")
    }

    override suspend fun editPlaylistMetadata(playlist: Playlist, title: String, description: String?) {
        if (!Prefixes.isLocal(playlist.id)) {
            throw ClientException.NotSupported("editPlaylistMetadata")
        }
        val settings = settings ?: return
        val updated = playlist.copy(title = title, description = description)
        LocalPlaylistStore.add(settings, updated)
        Logger.d("PlaylistEdit", "editPlaylistMetadata: ${playlist.id} -> $title")
    }

    override suspend fun addTracksToPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        index: Int,
        new: List<Track>
    ) {
        // Local playlist — existing behavior
        if (Prefixes.isLocal(playlist.id)) {
            val settings = settings ?: return
            LocalPlaylistTracksStore.addTracks(settings, playlist.id, new, index)
            Logger.d("PlaylistEdit", "addTracksToPlaylist (local): ${playlist.id}, +${new.size} at $index")
            return
        }

        // Saavn playlist

        val currentUser = SaavnDependencies.currentUserId
        val uid = playlist.extras["uid"].orEmpty()
        if (currentUser == null || uid.isBlank() || uid != currentUser) {
            throw Exception("Not authorized to edit this playlist")
        }

        val contents = new.joinToString(",") { track ->
            "~~${track.id}~${track.extras["language"].orEmpty()}"
        }

        Logger.d("PlaylistEdit", "addTracksToPlaylist (saavn): ${playlist.id}, +${new.size}")

        val response = api.playlistEdit.add(playlist.id, contents)

        val status = response["status"]?.jsonPrimitive?.contentOrNull
        if (status != "ok") {
            throw Exception("playlist.add failed: status=$status")
        }

        // Invalidate cache so next open re-fetches with the new tracks
        cachedPlaylistId = null
        cachedResponse = null
        cachedPlaylist = null
        cachedTracks = null

        Logger.d("PlaylistEdit", "addTracksToPlaylist (saavn): success, cache invalidated")
    }

    override suspend fun removeTracksFromPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        indexes: List<Int>
    ) {
        if (!Prefixes.isLocal(playlist.id)) {
            throw ClientException.NotSupported("removeTracksFromPlaylist")
        }
        val settings = settings ?: return
        LocalPlaylistTracksStore.removeTracks(settings, playlist.id, indexes)
        Logger.d("PlaylistEdit", "removeTracksFromPlaylist: ${playlist.id}, $indexes")
    }

    override suspend fun moveTrackInPlaylist(
        playlist: Playlist,
        tracks: List<Track>,
        fromIndex: Int,
        toIndex: Int
    ) {
        if (!Prefixes.isLocal(playlist.id)) {
            throw ClientException.NotSupported("moveTrackInPlaylist")
        }
        val settings = settings ?: return
        LocalPlaylistTracksStore.moveTrack(settings, playlist.id, fromIndex, toIndex)
        Logger.d("PlaylistEdit", "moveTrackInPlaylist: ${playlist.id}, $fromIndex -> $toIndex")
    }
}
