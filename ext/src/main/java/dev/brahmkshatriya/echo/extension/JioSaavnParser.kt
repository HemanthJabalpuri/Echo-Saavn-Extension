package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.*

import dev.brahmkshatriya.echo.extension.parser.*

import kotlinx.serialization.json.*

class JioSaavnParser : BaseParser() {

    private val trackParser = TrackParser()

    fun parseSongSearchResults(jsonString: String): List<Track> =
        trackParser.parseSongSearchResults(jsonString)

    fun parseSongDetails(jsonString: String): List<Track> =
        trackParser.parseSongDetails(jsonString)


    private val albumParser = AlbumParser(trackParser)

    fun parseAlbumToAlbum(obj: JsonObject): Album? =
        albumParser.parseAlbumToAlbum(obj)

    fun parseAlbumTracks(obj: JsonObject): List<Track> =
        albumParser.parseAlbumTracks(obj)
    
    fun parseAlbumSearchResults(jsonString: String): List<Album> =
        albumParser.parseAlbumSearchResults(jsonString)

    fun parseAlbumDetails(jsonString: String): Album? =
        albumParser.parseAlbumDetails(jsonString)

    fun parseAlbumTracksFromJson(jsonString: String): List<Track> =
        albumParser.parseAlbumTracksFromJson(jsonString)

    private val artistParser = ArtistParser(trackParser, albumParser)


    fun parseArtistToArtist(obj: JsonObject): Artist? =
        artistParser.parseArtistToArtist(obj)

    fun parseArtistTopSongs(obj: JsonObject): List<Track> =
        artistParser.parseArtistTopSongs(obj)

    fun parseArtistTopAlbums(obj: JsonObject): List<Album> =
        artistParser.parseArtistTopAlbums(obj)

    fun parseArtistIsRadioPresent(obj: JsonObject): Boolean =
        artistParser.parseArtistIsRadioPresent(obj)

    fun parseArtistSearchResults(jsonString: String): List<Artist> =
        artistParser.parseArtistSearchResults(jsonString)

    fun parseArtistDetails(jsonString: String): Artist? =
        artistParser.parseArtistDetails(jsonString)


    private val playlistParser = PlaylistParser(trackParser)

    // Existing delegations (updated return types)
    fun parsePlaylistSearchResults(jsonString: String): List<Playlist> =
        playlistParser.parsePlaylistSearchResults(jsonString)

    fun parsePlaylistDetails(jsonString: String): Playlist? =
        playlistParser.parsePlaylistDetails(jsonString)

    // New delegations (needed by PlaylistClientImpl)
    fun parsePlaylistToPlaylist(obj: JsonObject): Playlist? =
        playlistParser.parsePlaylistToPlaylist(obj)

    fun parsePlaylistTracks(obj: JsonObject): List<Track> =
        playlistParser.parsePlaylistTracks(obj)

    fun parsePlaylistTracksFromJson(jsonString: String): List<Track> =
        playlistParser.parsePlaylistTracksFromJson(jsonString)


    private val searchParser = SearchParser(trackParser, albumParser, artistParser, playlistParser)

    fun parseSearchAll(jsonString: String): SearchAllResult =
        searchParser.parseSearchAll(jsonString)


    private val homeParser = HomeParser(trackParser, albumParser, playlistParser)

    fun parseHomeData(jsonString: String): HomeData? =
        homeParser.parseHomeData(jsonString)


    private val radioParser = RadioParser(trackParser)

    fun parseStationId(jsonString: String): String? =
        radioParser.parseStationId(jsonString)

    fun parseSongSuggestions(jsonString: String): List<Track> =
        radioParser.parseSongSuggestions(jsonString)

    fun parseSimilarSongs(jsonString: String): List<Track> =
        radioParser.parseSimilarSongs(jsonString)

    fun parseRadioSongs(jsonString: String): List<Track> =
        radioParser.parseRadioSongs(jsonString)

    fun parseArtistRadioStationId(jsonString: String): String? =
        radioParser.parseArtistRadioStationId(jsonString)

}
