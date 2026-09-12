package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Track

import dev.brahmkshatriya.echo.extension.parser.*

class JioSaavnParser : BaseParser() {

    private val trackParser = TrackParser()

    fun parseSongSearchResults(jsonString: String): List<Track> =
        trackParser.parseSongSearchResults(jsonString)

    fun parseSongDetails(jsonString: String): List<Track> =
        trackParser.parseSongDetails(jsonString)


    private val albumParser = AlbumParser(trackParser)  // ← Pass trackParser

    fun parseAlbumSearchResults(jsonString: String): List<AlbumResult> =
        albumParser.parseAlbumSearchResults(jsonString)

    fun parseAlbumDetails(jsonString: String): AlbumDetail? =
        albumParser.parseAlbumDetails(jsonString)


    private val artistParser = ArtistParser(trackParser, albumParser)

    fun parseArtistSearchResults(jsonString: String): List<ArtistResult> =
        artistParser.parseArtistSearchResults(jsonString)

    fun parseArtistDetails(jsonString: String): ArtistDetail? =
        artistParser.parseArtistDetails(jsonString)


    private val playlistParser = PlaylistParser(trackParser)

    fun parsePlaylistSearchResults(jsonString: String): List<PlaylistResult> =
        playlistParser.parsePlaylistSearchResults(jsonString)

    fun parsePlaylistDetails(jsonString: String): PlaylistDetail? =
        playlistParser.parsePlaylistDetails(jsonString)


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
