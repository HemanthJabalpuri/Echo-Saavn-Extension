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
            songs = jsonObject["results"]?.jsonArray?.mapNotNull { trackParser.parseSongToTrack(it.jsonObject) } ?: emptyList(),
            albums = jsonObject["albums"]?.jsonObject?.get("data")?.jsonArray?.mapNotNull { albumParser.parseAlbumToAlbum(it.jsonObject) } ?: emptyList(),
            artists = artistParser.parseArtistResults(jsonObject["artists"]?.jsonObject?.get("data")?.jsonArray),
            playlists = playlistParser.parsePlaylistResults(jsonObject["playlists"]?.jsonObject?.get("data")?.jsonArray)
        )
    }
}
