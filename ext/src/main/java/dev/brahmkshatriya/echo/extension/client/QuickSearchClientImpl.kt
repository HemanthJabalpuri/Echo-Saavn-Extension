package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.QuickSearchClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

import dev.brahmkshatriya.echo.extension.*

class QuickSearchClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : QuickSearchClient {

    // ===== SHARED FETCHER =====
    private data class SearchResults(
        val songs: List<Track>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val playlists: List<Playlist>
    )

    private suspend fun fetchAll(query: String, limit: Int = 10): SearchResults = coroutineScope {
        val songsDeferred = async {
            try {
                val response = api.searchSongs(query, page = 1, limit = limit)
                parser.track.parseSongSearchResults(response)
            } catch (e: Exception) { emptyList() }
        }
        
        val albumsDeferred = async {
            try {
                val response = api.searchAlbums(query, page = 1, limit = limit)
                parser.album.parseAlbumSearchResults(response)
            } catch (e: Exception) { emptyList() }
        }
        
        val artistsDeferred = async {
            try {
                val response = api.searchArtists(query, page = 1, limit = limit)
                parser.artist.parseArtistSearchResults(response)
            } catch (e: Exception) { emptyList() }
        }
        
        val playlistsDeferred = async {
            try {
                val response = api.searchPlaylists(query, page = 1, limit = limit)
                parser.playlist.parsePlaylistSearchResults(response)
            } catch (e: Exception) { emptyList() }
        }
        
        SearchResults(
            songs = songsDeferred.await(),
            albums = albumsDeferred.await(),
            artists = artistsDeferred.await(),
            playlists = playlistsDeferred.await()
        )
    }

    // ===== QUICK SEARCH =====
    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        if (query.isBlank()) return emptyList()
        
        val results = fetchAll(query, limit = 5)
        
        val items = mutableListOf<QuickSearchItem>()
        results.songs.take(3).forEach { items.add(QuickSearchItem.Media(it, false)) }
        results.albums.take(3).forEach { items.add(QuickSearchItem.Media(it, false)) }
        results.artists.take(2).forEach { items.add(QuickSearchItem.Media(it, false)) }
        results.playlists.take(2).forEach { items.add(QuickSearchItem.Media(it, false)) }
        
        return items
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        // Not implemented
    }

    // ===== SEARCH FEED =====
    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        if (query.isBlank()) return emptyList<Shelf>().toFeed()
        
        val results = fetchAll(query, limit = 10)
        
        val shelves = mutableListOf<Shelf>()
        
        if (results.songs.isNotEmpty()) {
            shelves.add(Shelf.Lists.Tracks(
                id = "search_songs",
                title = "Songs",
                list = results.songs
            ))
        }
        
        if (results.albums.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "search_albums",
                title = "Albums",
                list = results.albums
            ))
        }
        
        if (results.artists.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "search_artists",
                title = "Artists",
                list = results.artists
            ))
        }
        
        if (results.playlists.isNotEmpty()) {
            shelves.add(Shelf.Lists.Items(
                id = "search_playlists",
                title = "Playlists",
                list = results.playlists
            ))
        }
        
        val tabs = listOf(
            Tab("all", "All"),
            Tab("songs", "Songs"),
            Tab("albums", "Albums"),
            Tab("artists", "Artists"),
            Tab("playlists", "Playlists")
        )
        
        return Feed(tabs) { tab ->
            when (tab?.id) {
                "songs" -> createSongsFeed(query).toFeedData()
                "albums" -> createAlbumsFeed(query).toFeedData()
                "artists" -> createArtistsFeed(query).toFeedData()
                "playlists" -> createPlaylistsFeed(query).toFeedData()
                else -> PagedData.Single { shelves }.toFeedData()
            }
        }
    }

    private fun createSongsFeed(query: String) = PagedData.Continuous<Shelf> { continuation ->
        val page = continuation?.toIntOrNull() ?: 1
        try {
            val response = api.searchSongs(query, page = page, limit = 20)
            val songs = parser.track.parseSongSearchResults(response)
            
            val items = songs.map { it.toShelf() }
            val nextContinuation = if (songs.size >= 20) (page + 1).toString() else null
            Page(items, nextContinuation)
        } catch (e: Exception) {
            Page(emptyList(), null)
        }
    }

    private fun createAlbumsFeed(query: String) = PagedData.Continuous<Shelf> { continuation ->
        val page = continuation?.toIntOrNull() ?: 1
        try {
            val response = api.searchAlbums(query, page = page, limit = 20)
            val albums = parser.album.parseAlbumSearchResults(response)
            
            val items = albums.map { it.toShelf() }
            val nextContinuation = if (albums.size >= 20) (page + 1).toString() else null
            Page(items, nextContinuation)
        } catch (e: Exception) {
            Page(emptyList(), null)
        }
    }

    private fun createArtistsFeed(query: String) = PagedData.Continuous<Shelf> { continuation ->
        val page = continuation?.toIntOrNull() ?: 1
        try {
            val response = api.searchArtists(query, page = page, limit = 20)
            val artists = parser.artist.parseArtistSearchResults(response)
            
            val items = artists.map { it.toShelf() }
            val nextContinuation = if (artists.size >= 20) (page + 1).toString() else null
            Page(items, nextContinuation)
        } catch (e: Exception) {
            Page(emptyList(), null)
        }
    }

    private fun createPlaylistsFeed(query: String) = PagedData.Continuous<Shelf> { continuation ->
        val page = continuation?.toIntOrNull() ?: 1
        try {
            val response = api.searchPlaylists(query, page = page, limit = 20)
            val playlists = parser.playlist.parsePlaylistSearchResults(response)
            
            val items = playlists.map { it.toShelf() }
            val nextContinuation = if (playlists.size >= 20) (page + 1).toString() else null
            Page(items, nextContinuation)
        } catch (e: Exception) {
            Page(emptyList(), null)
        }
    }
}
