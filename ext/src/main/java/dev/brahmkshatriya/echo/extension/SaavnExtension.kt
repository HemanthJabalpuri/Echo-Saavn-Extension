package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.clients.*
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.settings.Setting
import dev.brahmkshatriya.echo.common.settings.Settings

import dev.brahmkshatriya.echo.extension.client.*

class SaavnExtension : ExtensionClient, 
    QuickSearchClient, HomeFeedClient, LibraryFeedClient,
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
    
    private val homeFeedClient = HomeFeedClientImpl(api, parser)

    // HomeFeedClient delegation
    override suspend fun loadHomeFeed(): Feed<Shelf> {
        return homeFeedClient.loadHomeFeed()
    }

    //============= QUICK SEARCH =============

    private val quickSearchClient = QuickSearchClientImpl(api, parser)

    // QuickSearchClient delegation
    override suspend fun quickSearch(query: String): List<QuickSearchItem> {
        return quickSearchClient.quickSearch(query)
    }

    override suspend fun deleteQuickSearch(item: QuickSearchItem) {
        quickSearchClient.deleteQuickSearch(item)
    }

    //============= SEARCH FEED =============

    override suspend fun loadSearchFeed(query: String): Feed<Shelf> {
        return quickSearchClient.loadSearchFeed(query)
    }
    
    
    //============= TRACK CLIENT =============

    private val trackClient = TrackClientImpl(api, parser)

    // TrackClient delegation
    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return trackClient.loadTrack(track, isDownload)
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable, isDownload: Boolean
    ): Streamable.Media {
        return trackClient.loadStreamableMedia(streamable, isDownload)
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        return trackClient.loadFeed(track)
    }

    //============= ALBUM CLIENT =============
    
    private val albumClient = AlbumClientImpl(api, parser)

    // AlbumClient delegation
    override suspend fun loadAlbum(album: Album): Album {
        return albumClient.loadAlbum(album)
    }

    override suspend fun loadTracks(album: Album): Feed<Track>? {
        return albumClient.loadTracks(album)
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        return albumClient.loadFeed(album)
    }

    //============= ARTIST CLIENT =============

    private val artistClient = ArtistClientImpl(api, parser)

    // ArtistClient delegation
    override suspend fun loadArtist(artist: Artist): Artist {
        return artistClient.loadArtist(artist)
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return artistClient.loadFeed(artist)
    }

    //============= PLAYLIST CLIENT =============

    private val playlistClient = PlaylistClientImpl(api, parser)

    // PlaylistClient delegation
    override suspend fun loadPlaylist(playlist: Playlist): Playlist {
        return playlistClient.loadPlaylist(playlist)
    }

    override suspend fun loadTracks(playlist: Playlist): Feed<Track> {
        return playlistClient.loadTracks(playlist)
    }

    override suspend fun loadFeed(playlist: Playlist): Feed<Shelf>? {
        return playlistClient.loadFeed(playlist)
    }

    //============= RADIO CLIENT =============
    
    private val radioClient = RadioClientImpl(api, parser)

    // RadioClient delegation
    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return radioClient.radio(item, context)
    }
    
    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        return radioClient.loadTracks(radio)
    }
    
    override suspend fun loadRadio(radio: Radio): Radio {
        return radioClient.loadRadio(radio)
    }

    //============= SHARE CLIENT =============
    
    private val shareClient = ShareClientImpl()
    
    override suspend fun onShare(item: EchoMediaItem): String {
        return shareClient.onShare(item)
    }

    //============= LIBRARY FEED CLIENT =============

    private val libraryFeedClient = LibraryFeedClientImpl()

    // LibraryFeedClient delegation
    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        return libraryFeedClient.loadLibraryFeed()
    }
}
