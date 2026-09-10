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

    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        if (query.isBlank()) return emptyList()
        
        return try {
            val response = api.searchAll(query, page = 1, limit = 10)
            val results = parser.parseSearchAll(response)
            
            val items = mutableListOf<QuickSearchItem>()
            results.songs.take(3).forEach { song ->
                items.add(QuickSearchItem.Media(songResultToTrack(song), false))
            }
            results.albums.take(3).forEach { album ->
                items.add(QuickSearchItem.Media(albumResultToAlbum(album), false))
            }
            results.artists.take(2).forEach { artist ->
                items.add(QuickSearchItem.Media(artistResultToArtist(artist), false))
            }
            results.playlists.take(2).forEach { playlist ->
                items.add(QuickSearchItem.Media(playlistResultToPlaylist(playlist), false))
            }
            
            items
        } catch (e: Exception) {
            println("DEBUG: Quick search failed: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        // Not implemented - search history not stored
    }

    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        if (query.isBlank()) {
            return emptyList<Shelf>().toFeed()
        }
        
        return coroutineScope {
            val songsDeferred = async { 
                try { 
                    val response = api.searchSongs(query, page = 1, limit = 10)
                    parser.parseSongSearchResults(response)
                } catch (e: Exception) { 
                    println("DEBUG: Songs search failed: ${e.message}")
                    emptyList() 
                }
            }
            
            val albumsDeferred = async { 
                try { 
                    val response = api.searchAlbums(query, page = 1, limit = 10)
                    parser.parseAlbumSearchResults(response)
                } catch (e: Exception) { 
                    println("DEBUG: Albums search failed: ${e.message}")
                    emptyList() 
                }
            }
            
            val artistsDeferred = async { 
                try { 
                    val response = api.searchArtists(query, page = 1, limit = 10)
                    parser.parseArtistSearchResults(response)
                } catch (e: Exception) { 
                    println("DEBUG: Artists search failed: ${e.message}")
                    emptyList() 
                }
            }
            
            val playlistsDeferred = async { 
                try { 
                    val response = api.searchPlaylists(query, page = 1, limit = 10)
                    parser.parsePlaylistSearchResults(response)
                } catch (e: Exception) { 
                    println("DEBUG: Playlists search failed: ${e.message}")
                    emptyList() 
                }
            }
            
            val songs = songsDeferred.await()
            val albums = albumsDeferred.await()
            val artists = artistsDeferred.await()
            val playlists = playlistsDeferred.await()
            
            val shelves = mutableListOf<Shelf>()
            
            if (songs.isNotEmpty()) {
                shelves.add(Shelf.Lists.Tracks(
                    id = "search_songs",
                    title = "Songs",
                    list = songs.map { songResultToTrack(it) }
                ))
            }
            
            if (albums.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "search_albums",
                    title = "Albums",
                    list = albums.map { albumResultToAlbum(it) }
                ))
            }
            
            if (artists.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "search_artists",
                    title = "Artists",
                    list = artists.map { artistResultToArtist(it) }
                ))
            }
            
            if (playlists.isNotEmpty()) {
                shelves.add(Shelf.Lists.Items(
                    id = "search_playlists",
                    title = "Playlists",
                    list = playlists.map { playlistResultToPlaylist(it) }
                ))
            }
            
            val tabs = listOf(
                Tab("all", "All"),
                Tab("songs", "Songs"),
                Tab("albums", "Albums"),
                Tab("artists", "Artists"),
                Tab("playlists", "Playlists")
            )
            
            Feed(tabs) { tab ->
                when (tab?.id) {
                    "songs" -> createSongsFeed(query).toFeedData()
                    "albums" -> createAlbumsFeed(query).toFeedData()
                    "artists" -> createArtistsFeed(query).toFeedData()
                    "playlists" -> createPlaylistsFeed(query).toFeedData()
                    else -> PagedData.Single { shelves }.toFeedData()
                }
            }
        }
    }
    
    private fun createSongsFeed(query: String) = PagedData.Continuous { continuation ->
        val page = (continuation as? String)?.toIntOrNull() ?: 1
        try {
            val response = api.searchSongs(query, page = page, limit = 20)
            val songs = parser.parseSongSearchResults(response)
            
            Page(
                listOf(Shelf.Lists.Tracks(
                    id = "search_songs_tab",
                    title = "",
                    list = songs.map { songResultToTrack(it) }
                )),
                if (songs.size >= 20) (page + 1).toString() else null
            )
        } catch (e: Exception) {
            println("DEBUG: Songs tab pagination failed: ${e.message}")
            Page(emptyList<Shelf>(), null)
        }
    }
    
    private fun createAlbumsFeed(query: String) = PagedData.Continuous { continuation ->
        val page = (continuation as? String)?.toIntOrNull() ?: 1
        try {
            val response = api.searchAlbums(query, page = page, limit = 20)
            val albums = parser.parseAlbumSearchResults(response)
            
            Page(
                listOf(Shelf.Lists.Items(
                    id = "search_albums_tab",
                    title = "",
                    list = albums.map { albumResultToAlbum(it) }
                )),
                if (albums.size >= 20) (page + 1).toString() else null
            )
        } catch (e: Exception) {
            println("DEBUG: Albums tab pagination failed: ${e.message}")
            Page(emptyList<Shelf>(), null)
        }
    }
    
    private fun createArtistsFeed(query: String) = PagedData.Continuous { continuation ->
        val page = (continuation as? String)?.toIntOrNull() ?: 1
        try {
            val response = api.searchArtists(query, page = page, limit = 20)
            val artists = parser.parseArtistSearchResults(response)
            
            Page(
                listOf(Shelf.Lists.Items(
                    id = "search_artists_tab",
                    title = "",
                    list = artists.map { artistResultToArtist(it) }
                )),
                if (artists.size >= 20) (page + 1).toString() else null
            )
        } catch (e: Exception) {
            println("DEBUG: Artists tab pagination failed: ${e.message}")
            Page(emptyList<Shelf>(), null)
        }
    }
    
    private fun createPlaylistsFeed(query: String) = PagedData.Continuous { continuation ->
        val page = (continuation as? String)?.toIntOrNull() ?: 1
        try {
            val response = api.searchPlaylists(query, page = page, limit = 20)
            val playlists = parser.parsePlaylistSearchResults(response)
            
            Page(
                listOf(Shelf.Lists.Items(
                    id = "search_playlists_tab",
                    title = "",
                    list = playlists.map { playlistResultToPlaylist(it) }
                )),
                if (playlists.size >= 20) (page + 1).toString() else null
            )
        } catch (e: Exception) {
            println("DEBUG: Playlists tab pagination failed: ${e.message}")
            Page(emptyList<Shelf>(), null)
        }
    }

}
