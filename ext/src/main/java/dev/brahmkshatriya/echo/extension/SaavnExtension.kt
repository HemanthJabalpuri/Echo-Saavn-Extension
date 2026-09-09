package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.*
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.MediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SaavnExtension : ExtensionClient, 
    QuickSearchClient, SearchFeedClient, HomeFeedClient, LibraryFeedClient,
    TrackClient, AlbumClient, ArtistClient, PlaylistClient,
    RadioClient, ShareClient {

    private val api by lazy { JioSaavnApi() }
    private val parser by lazy { JioSaavnParser() }
    
    private lateinit var settings: Settings
    
    override suspend fun getSettingItems(): List<Setting> {
        return emptyList()
    }
    
    override fun setSettings(settings: Settings) {
        this.settings = settings
    }

    //============= HOME FEED =============
    
    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val tabs = listOf(
            Tab(id = "hindi", title = "Hindi"),
            Tab(id = "english", title = "English"),
            Tab(id = "punjabi", title = "Punjabi"),
            Tab(id = "tamil", title = "Tamil"),
            Tab(id = "telugu", title = "Telugu"),
            Tab(id = "marathi", title = "Marathi"),
            Tab(id = "gujarati", title = "Gujarati"),
            Tab(id = "bengali", title = "Bengali"),
            Tab(id = "kannada", title = "Kannada"),
            Tab(id = "bhojpuri", title = "Bhojpuri"),
            Tab(id = "malayalam", title = "Malayalam"),
            Tab(id = "urdu", title = "Urdu")
        )
        
        return Feed(tabs) { tab ->
            try {
                val language = tab?.id ?: "hindi"
                val response = api.getHomeData(language)
                val homeData = parser.parseHomeData(response) 
                
                if (homeData == null) {
                    return@Feed emptyList<Shelf>().toFeedData()
                }
                
                val shelves = mutableListOf<Shelf>()
                if (homeData.nowTrending.isNotEmpty()) {
                    val trendingItems = homeData.nowTrending.map { mediaItem ->
                        when (mediaItem) {
                            is MediaItem.Song -> songResultToTrack(mediaItem.data)
                            is MediaItem.Album -> albumResultToAlbum(mediaItem.data)
                            is MediaItem.Playlist -> playlistResultToPlaylist(mediaItem.data)
                        }
                    }
                    shelves.add(Shelf.Lists.Items(
                        id = "now_trending",
                        title = "Now Trending",
                        list = trendingItems,
                        subtitle = "Popular content right now"
                    ))
                }
                if (homeData.topPlaylists.isNotEmpty()) {
                    shelves.add(Shelf.Lists.Items(
                        id = "top_playlists",
                        title = "Top Playlists",
                        list = homeData.topPlaylists.map { playlistResultToPlaylist(it) },
                        subtitle = "Curated playlists for you"
                    ))
                }

                if (homeData.newAlbums.isNotEmpty()) {
                    val newAlbumItems = homeData.newAlbums.map { mediaItem ->
                        when (mediaItem) {
                            is MediaItem.Album -> albumResultToAlbum(mediaItem.data)
                            is MediaItem.Song -> songResultToTrack(mediaItem.data)
                            is MediaItem.Playlist -> playlistResultToPlaylist(mediaItem.data)
                        }
                    }
                    shelves.add(Shelf.Lists.Items(
                        id = "new_albums",
                        title = "New Albums",
                        list = newAlbumItems,
                        subtitle = "Latest releases"
                    ))
                }

                if (homeData.topCharts.isNotEmpty()) {
                    shelves.add(Shelf.Lists.Items(
                        id = "top_charts",
                        title = "Top Charts",
                        list = homeData.topCharts.map { playlistResultToPlaylist(it) },
                        subtitle = "Trending charts"
                    ))
                }
                
                shelves.toFeedData()
            } catch (e: Exception) {
                println("DEBUG: Failed to load home feed for tab ${tab?.id}: ${e.message}")
                e.printStackTrace()
                emptyList<Shelf>().toFeedData()
            }
        }
    }

    //============= QUICK SEARCH =============
    
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
        //Not implemented - search history not stored
    }

    //============= SEARCH FEED =============
    
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

    //============= TRACK CLIENT =============
    
    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return try {
            println("DEBUG: Loading track with ID: ${track.id}")
            val response = api.getSongDetails(track.id)
            val songDetails = parser.parseSongDetails(response).firstOrNull()
                ?: throw Exception("Track not found")
            
            songDetailToTrack(songDetails)
        } catch (e: Exception) {
            println("DEBUG: Failed to load track ${track.id}: ${e.message}")
            throw Exception("Failed to load track: ${e.message}")
        }
    }
    
    override suspend fun loadStreamableMedia(
        streamable: Streamable, 
        isDownload: Boolean
    ): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                println("DEBUG: Loading streamable media")
                
                val streamUrls = streamable.extras["streamUrls"] 
                    ?: throw Exception("No stream URLs found")
                val urls = parseStreamUrls(streamUrls)

                val sources = mutableListOf<Streamable.Source.Http>()
                
                if (urls["veryHigh"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["veryHigh"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 320,
                        title = "320kbps"
                    ))
                }
                
                if (urls["high"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["high"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 160,
                        title = "160kbps"
                    ))
                }
                
                if (urls["medium"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["medium"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 96,
                        title = "96kbps"
                    ))
                }
                
                if (urls["low"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["low"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 48,
                        title = "48kbps"
                    ))
                }
                
                if (sources.isEmpty()) {
                    throw Exception("No valid stream URLs available")
                }
                
                Streamable.Media.Server(sources, false)
            }
            Streamable.MediaType.Background -> {
                throw Exception("Background streamables not supported")
            }
            Streamable.MediaType.Subtitle -> {
                throw Exception("Subtitles not supported")
            }
        }
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        return try {
            println("DEBUG: Loading related tracks for: ${track.id}")

            try {
                val songId = track.extras["songId"] ?: track.id
                val stationResponse = api.createSongStation(songId)
                val stationId = parser.parseStationId(stationResponse)
                
                if (stationId != null) {
                    val suggestionsResponse = api.getSongSuggestions(stationId, limit = 20)
                    val songs = parser.parseSongSuggestions(suggestionsResponse)
                    
                    if (songs.isNotEmpty()) {
                        val tracks = songs.map { songResultToTrack(it) }
                        
                        return listOf(
                            Shelf.Lists.Tracks(
                                id = "similar_tracks",
                                title = "Similar Tracks",
                                list = tracks,
                                subtitle = "You might also like"
                            )
                        ).toFeed()
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Station-based suggestions failed in loadFeed: ${e.message}")
            }
            
            emptyList<Shelf>().toFeed()
        } catch (e: Exception) {
            println("DEBUG: Failed to load similar tracks: ${e.message}")
            emptyList<Shelf>().toFeed()
        }
    }

    //============= ALBUM CLIENT =============
    
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
            
            val tracks = albumDetail.songs.map { songDetailToTrack(it) }
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load album tracks: ${e.message}")
            null
        }
    }
    
    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        return null
    }

    //============= ARTIST CLIENT =============

    override suspend fun loadArtist(artist: Artist): Artist {
        return try {
            println("DEBUG: Loading artist with ID: ${artist.id}")
            // Fetch only 10 songs for the initial metadata
            val response = api.getArtistDetails(artist.id, songCount = 10, albumCount = 10, page = 1)
            val artistDetail = parser.parseArtistDetails(response)
                ?: throw Exception("Artist not found")
            
            artistDetailToArtist(artistDetail)
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist ${artist.id}: ${e.message}")
            throw Exception("Failed to load artist: ${e.message}")
        }
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return try {
            // Fetch first page of songs and albums
            val initialResponse = api.getArtistDetails(artist.id, songCount = 50, albumCount = 50, page = 1)
            val artistDetail = parser.parseArtistDetails(initialResponse)
                ?: return emptyList<Shelf>().toFeed()

            val shelves = mutableListOf<Shelf>()

            // Songs shelf with "More" button
            val firstPageSongs = artistDetail.topSongs.map { songDetailToTrack(it) }
            if (firstPageSongs.isNotEmpty()) {
                shelves.add(
                    Shelf.Lists.Items(
                        id = "artist_songs",
                        title = "Popular Songs",
                        list = firstPageSongs,
                        subtitle = "${firstPageSongs.size} songs",
                        more = if (firstPageSongs.size >= 50) createMoreFeed(artist, "songs") else null
                    )
                )
            }

            // Albums shelf with "More" button
            val firstPageAlbums = artistDetail.topAlbums.map { albumResultToAlbum(it) }
            if (firstPageAlbums.isNotEmpty()) {
                shelves.add(
                    Shelf.Lists.Items(
                        id = "artist_albums",
                        title = "Albums",
                        list = firstPageAlbums,
                        subtitle = "${firstPageAlbums.size} albums",
                        more = if (firstPageAlbums.size >= 50) createMoreFeed(artist, "albums") else null
                    )
                )
            }

            shelves.toFeed()
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist feed: ${e.message}")
            emptyList<Shelf>().toFeed()
        }
    }

    private fun createMoreFeed(artist: Artist, type: String): Feed<Shelf> {
        return Feed(emptyList()) { _ ->
            Feed.Data(
                PagedData.Continuous<Shelf> { continuation ->
                    val page = continuation?.toIntOrNull() ?: 1
                    
                    try {
                        val response = api.getArtistDetails(
                            artist.id,
                            songCount = if (type == "songs") 50 else 0,
                            albumCount = if (type == "albums") 50 else 0,
                            page = page
                        )
                        val detail = parser.parseArtistDetails(response)
                            ?: return@Continuous Page(emptyList(), null)
                        
                        val items = when (type) {
                            "songs" -> detail.topSongs.map { songDetailToTrack(it).toShelf() }
                            "albums" -> detail.topAlbums.map { albumResultToAlbum(it).toShelf() }
                            else -> emptyList()
                        }
                        
                        // If we got exactly 50 items, assume there's a next page
                        val nextContinuation = if (items.size >= 50) {
                            (page + 1).toString()
                        } else {
                            null
                        }
                        
                        Page(items, nextContinuation)
                        
                    } catch (e: Exception) {
                        Page(emptyList(), null)
                    }
                }
            )
        }
    }

    //============= PLAYLIST CLIENT =============
    
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
    
    //============= RADIO CLIENT =============
    
    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return when (item) {
            is Track -> createRadioFromTrack(item)
            is Album -> createRadioFromAlbum(item)
            is Artist -> createRadioFromArtist(item)
            is Playlist -> createRadioFromPlaylist(item)
            else -> throw Exception("Radio not supported for this item type")
        }
    }
    
    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        val tracksJson = radio.extras["tracks"] ?: return emptyList<Track>().toFeed() as Feed<Track>
        return try {
            val trackIds = tracksJson.split(",")
            val tracks = mutableListOf<Track>()
            
            for (id in trackIds) {
                try {
                    val response = api.getSongDetails(id)
                    val songDetail = parser.parseSongDetails(response).firstOrNull()
                    if (songDetail != null) {
                        tracks.add(songDetailToTrack(songDetail))
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to load track $id for radio: ${e.message}")
                }
            }
            
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load radio tracks: ${e.message}")
            emptyList<Track>().toFeed() as Feed<Track>
        }
    }
    
    override suspend fun loadRadio(radio: Radio): Radio = radio
    
    private suspend fun createRadioFromTrack(track: Track): Radio {
        return try {
            println("DEBUG: Creating radio from track: id=${track.id}, title=${track.title}")
            
            try {
                val stationResponse = api.createSongStation(track.id)
                val stationId = parser.parseStationId(stationResponse)
                
                if (stationId != null) {
                    println("DEBUG: Created station with ID: $stationId")
        
                    val suggestionsResponse = api.getSongSuggestions(stationId, limit = 50)
                    val songs = parser.parseSongSuggestions(suggestionsResponse)
                    
                    if (songs.isNotEmpty()) {
                        println("DEBUG: Successfully got ${songs.size} song suggestions from station")
                        val trackIds = songs.map { it.id }.joinToString(",")
                        
                        return Radio(
                            id = "radio_${track.id}",
                            title = "${track.title} Radio",
                            subtitle = "Similar to ${track.title}",
                            cover = track.cover,
                            extras = mapOf("tracks" to trackIds)
                        )
                    } else {
                        println("DEBUG: Station created but no suggestions returned")
                    }
                } else {
                    println("DEBUG: Failed to create station, stationId is null")
                }
            } catch (e: Exception) {
                println("DEBUG: Station-based suggestions failed: ${e.message}, falling back to artist radio")
            }
            
            val artists = track.artists
            if (artists.isEmpty()) {
                throw Exception("No artists found for this track")
            }
            
            val primaryArtist = artists.first()
            println("DEBUG: Creating artist-based radio for: ${primaryArtist.name}")
            
            val artistResponse = api.getArtistDetails(primaryArtist.id, songCount = 50, albumCount = 0)
            val artistDetail = parser.parseArtistDetails(artistResponse)
                ?: throw Exception("Could not load artist details")
            
            if (artistDetail.topSongs.isEmpty()) {
                throw Exception("Artist has no songs available")
            }
            
            val trackIds = artistDetail.topSongs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${track.id}",
                title = "${track.title} Radio",
                subtitle = "Songs by ${primaryArtist.name}",
                cover = track.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from track: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }
    
    private suspend fun createRadioFromAlbum(album: Album): Radio {
        return try {
            val response = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetails(response)
                ?: throw Exception("Album not found")
            
            val trackIds = albumDetail.songs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${album.id}",
                title = "${album.title} Radio",
                subtitle = "Songs from ${album.title}",
                cover = album.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from album: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }
    
    internal suspend fun createRadioFromArtist(artist: Artist): Radio {
        return try {
            // Fetch artist details to check radio availability
            val response = api.getArtistDetails(artist.id, songCount = 50, albumCount = 10)
            val artistDetail = parser.parseArtistDetails(response)
                ?: throw Exception("Artist not found")
            
            // Check if radio is available
            if (artistDetail.isRadioPresent) {
                try {
                    // Step 1: Create radio station
                    val language = artistDetail.dominantLanguage.takeIf { it.isNotBlank() } ?: "hindi"
                    val stationResponse = api.createArtistRadioStation(artistDetail.name, language)
                    val stationId = parser.parseArtistRadioStationId(stationResponse)
                    
                    if (stationId != null) {
                        // Step 2: Get radio songs
                        val songsResponse = api.getRadioSongs(stationId, limit = 20)
                        val radioSongs = parser.parseRadioSongs(songsResponse)
                        
                        if (radioSongs.isNotEmpty()) {
                            val trackIds = radioSongs.map { it.id }.joinToString(",")
                            
                            return Radio(
                                id = "radio_${artist.id}",
                                title = "${artist.name} Radio",
                                subtitle = "Radio station for ${artist.name}",
                                cover = artist.cover,
                                extras = mapOf("tracks" to trackIds)
                            )
                        }
                    }
                    
                    // If we reach here, something failed in radio creation
                    println("DEBUG: Radio creation failed for ${artist.name}, falling back to top songs")
                    
                } catch (e: Exception) {
                    println("DEBUG: Radio API failed for ${artist.name}: ${e.message}, falling back to top songs")
                }
            }
            
            // Fallback: Use top songs
            val trackIds = artistDetail.topSongs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${artist.id}",
                title = "${artist.name} Radio",
                subtitle = "Top songs by ${artist.name}",
                cover = artist.cover,
                extras = mapOf("tracks" to trackIds)
            )
            
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from artist: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }

    private suspend fun createRadioFromPlaylist(playlist: Playlist): Radio {
        return try {
            val response = api.getPlaylistDetails(playlist.id)
            val playlistDetail = parser.parsePlaylistDetails(response)
                ?: throw Exception("Playlist not found")
            
            val trackIds = playlistDetail.songs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${playlist.id}",
                title = "${playlist.title} Radio",
                subtitle = "Songs from ${playlist.title}",
                cover = playlist.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from playlist: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }
    
    //============= SHARE CLIENT =============
    
    override suspend fun onShare(item: EchoMediaItem): String {
        return when (item) {
            is Track -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/song/${item.id}"
            is Album -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/album/${item.id}"
            is Artist -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/artist/${item.id}"
            is Playlist -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/featured/${item.id}"
            else -> throw Exception("Sharing not supported for this item type")
        }
    }
    
    //============= LIBRARY FEED CLIENT =============
    
    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        println("DEBUG: loadLibraryFeed() called - Creating library feed")
        
        return try {
            val shelves = mutableListOf<Shelf>()
            val favoritesCategory = Shelf.Category(
                id = "favorites",
                title = "Favorites",
                subtitle = "Your favorite music",
                feed = null,
                extras = mapOf("type" to "favorites")
            )
            shelves.add(favoritesCategory)
            
            val recentlyPlayedCategory = Shelf.Category(
                id = "recently_played",
                title = "Recently Played",
                subtitle = "Your recently played tracks",
                feed = null,
                extras = mapOf("type" to "recently_played")
            )
            shelves.add(recentlyPlayedCategory)
            
            val playlistsCategory = Shelf.Category(
                id = "playlists",
                title = "My Playlists",
                subtitle = "Your created playlists",
                feed = null,
                extras = mapOf("type" to "playlists")
            )
            shelves.add(playlistsCategory)
            
            println("DEBUG: Created library feed with ${shelves.size} category shelves")
            shelves.toFeed()
            
        } catch (e: Exception) {
            println("DEBUG: Error in loadLibraryFeed: ${e.message}")
            e.printStackTrace()
            
            val errorShelf = Shelf.Category(
                id = "library_error",
                title = "Library",
                subtitle = "Unable to load library content",
                feed = null
            )
            listOf(errorShelf).toFeed()
        }
    }

}
