package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.common.helpers.ClientException

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.storage.*
import dev.brahmkshatriya.echo.extension.utils.Logger

class LibraryFeedClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser,
) : LibraryFeedClient {

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val tabs = buildList {
            add(Tab(id = "local", title = "Local"))
            if (SaavnDependencies.sessionCookie != null) {
                add(Tab(id = "saavn", title = "JioSaavn"))
            }
        }

        return Feed(tabs) { tab ->
            when (tab?.id) {
                "saavn" -> loadSaavnTab()
                else -> loadLocalTab()
            }
        }
    }

    // ===== LOCAL TAB (unchanged behavior) =====

    private fun loadLocalTab(): Feed.Data<Shelf> {
        val settings = SaavnDependencies.settings
        val shelves = if (settings != null) buildLocalShelves(settings) else emptyList()
        return shelves.toFeedData()
    }

    private fun buildLocalShelves(settings: Settings): List<Shelf> {
        val shelves = mutableListOf<Shelf>()

        val recent = LocalRecentStore.getAll(settings)
        if (recent.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "recent_tracks",
                title = "Recently Played",
                list = recent,
                subtitle = "${recent.size} tracks"
            ))
        }

        val myPlaylists = LocalPlaylistStore.getAll(settings)
        if (myPlaylists.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "my_playlists",
                title = "My Playlists",
                list = myPlaylists,
                subtitle = "${myPlaylists.size} playlists"
            ))
        }

        val tracks = LocalLikedTracksStore.getAll(settings)
        if (tracks.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "liked_tracks",
                title = "Liked Songs",
                list = tracks,
                subtitle = "${tracks.size} tracks"
            ))
        }

        val albums = LocalLikedAlbumsStore.getAll(settings)
        if (albums.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "liked_albums",
                title = "Liked Albums",
                list = albums,
                subtitle = "${albums.size} albums"
            ))
        }

        val artists = LocalLikedArtistsStore.getAll(settings)
        if (artists.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "liked_artists",
                title = "Liked Artists",
                list = artists,
                subtitle = "${artists.size} artists"
            ))
        }

        val playlists = LocalLikedPlaylistsStore.getAll(settings)
        if (playlists.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "liked_playlists",
                title = "Liked Playlists",
                list = playlists,
                subtitle = "${playlists.size} playlists"
            ))
        }

        return shelves
    }

    // ===== SAAVN TAB =====

    private suspend fun loadSaavnTab(): Feed.Data<Shelf> {
        return try {
            buildSaavnShelves().toFeedData()
        } catch (e: ClientException.LoginRequired) {
            throw e
        } catch (e: ClientException.Unauthorized) {
            throw e
        } catch (e: Exception) {
            Logger.e("LibraryFeed", "Failed to load Saavn tab", e)
            emptyList<Shelf>().toFeedData()
        }
    }

    private suspend fun buildSaavnShelves(): List<Shelf> {
        val shelves = mutableListOf<Shelf>()
        val library = api.library.getAll()

        // ---- Liked Songs ----
        val songIds = parser.library.parseSongIds(library)
        if (songIds.isNotEmpty()) {
            val tracks = fetchSongDetailsChunked(songIds)
            if (tracks.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "saavn_liked_tracks",
                    title = "Liked Songs",
                    list = tracks,
                    subtitle = "${tracks.size} tracks"
                ))
            }
        }

        // ---- Liked Albums ----
        val albumIds = parser.library.parseAlbumIds(library)
        if (albumIds.isNotEmpty()) {
            val albums = fetchAlbumDetailsChunked(albumIds)
            if (albums.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "saavn_liked_albums",
                    title = "Liked Albums",
                    list = albums,
                    subtitle = "${albums.size} albums"
                ))
            }
        }

        // ---- Followed Artists ----
        val artistIds = parser.library.parseArtistIds(library)
        if (artistIds.isNotEmpty()) {
            val artists = fetchArtistDetailsChunked(artistIds)
            if (artists.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "saavn_followed_artists",
                    title = "Followed Artists",
                    list = artists,
                    subtitle = "${artists.size} artists"
                ))
            }
        }

        // ---- Playlists ----
        val playlistEntries = parser.library.parsePlaylistEntries(library)
        if (playlistEntries.isNotEmpty()) {
            val playlists = fetchPlaylistDetailsChunked(playlistEntries.map { it.id })
            if (playlists.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "saavn_playlists",
                    title = "Playlists",
                    list = playlists,
                    subtitle = "${playlists.size} playlists"
                ))
            }
        }

        return shelves
    }

    // ===== Chunked fetchers =====

    private suspend fun fetchSongDetailsChunked(ids: List<String>): List<Track> {
        val result = mutableListOf<Track>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            try {
                val resp = api.library.getDetails(chunk, "song", chunk.size)
                result.addAll(parser.track.parseSongDetails(resp))
            } catch (e: Exception) {
                Logger.e("LibraryFeed", "Failed to fetch ${chunk.size} songs", e)
            }
        }
        return result
    }

    private suspend fun fetchAlbumDetailsChunked(ids: List<String>): List<Album> {
        val result = mutableListOf<Album>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            try {
                val resp = api.library.getDetails(chunk, "album", chunk.size)
                result.addAll(parser.album.parseAlbumResults(resp))
            } catch (e: Exception) {
                Logger.e("LibraryFeed", "Failed to fetch ${chunk.size} albums", e)
            }
        }
        return result
    }

    private suspend fun fetchArtistDetailsChunked(ids: List<String>): List<Artist> {
        val result = mutableListOf<Artist>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            try {
                val resp = api.library.getDetails(chunk, "artist", chunk.size)
                result.addAll(parser.artist.parseArtistSearchResults(resp))
            } catch (e: Exception) {
                Logger.e("LibraryFeed", "Failed to fetch ${chunk.size} artists", e)
            }
        }
        return result
    }

    private suspend fun fetchPlaylistDetailsChunked(ids: List<String>): List<Playlist> {
        val result = mutableListOf<Playlist>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            try {
                val resp = api.library.getDetails(chunk, "playlist", chunk.size)
                result.addAll(parser.playlist.parsePlaylistResults(resp))
            } catch (e: Exception) {
                Logger.e("LibraryFeed", "Failed to fetch ${chunk.size} playlists", e)
            }
        }
        return result
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}
