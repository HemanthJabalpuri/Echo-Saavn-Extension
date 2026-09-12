package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*

class SearchParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val artistParser: ArtistParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseSearchAll(jsonString: String): SearchAllResult {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        
        return SearchAllResult(
            songs = trackParser.parseSongResults(jsonObject["results"]?.jsonArray),
            albums = albumParser.parseAlbumResults(jsonObject["albums"]?.jsonObject?.get("data")?.jsonArray),
            artists = artistParser.parseArtistResults(jsonObject["artists"]?.jsonObject?.get("data")?.jsonArray),
            playlists = playlistParser.parsePlaylistResults(jsonObject["playlists"]?.jsonObject?.get("data")?.jsonArray)
        )
    }
}
