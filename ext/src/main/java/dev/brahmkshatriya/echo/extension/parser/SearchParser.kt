package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*

import kotlinx.serialization.json.*

data class SearchAllResult(
    val songs: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<Playlist>
)

class SearchParser(
    private val trackParser: TrackParser,
    private val albumParser: AlbumParser,
    private val artistParser: ArtistParser,
    private val playlistParser: PlaylistParser
) : BaseParser() {

    fun parseSearchAll(jsonString: String): SearchAllResult {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        return SearchAllResult(
            songs = jsonObject["results"]?.jsonArray?.mapNotNull {
                trackParser.parseSongToTrack(it.jsonObject)
            } ?: emptyList(),
            albums = jsonObject["albums"]?.jsonObject?.get("data")?.jsonArray?.mapNotNull {
                albumParser.parseAlbumToAlbum(it.jsonObject)
            } ?: emptyList(),
            artists = jsonObject["artists"]?.jsonObject?.get("data")?.jsonArray?.mapNotNull {
                artistParser.parseArtistToArtist(it.jsonObject)
            } ?: emptyList(),
            playlists = jsonObject["playlists"]?.jsonObject?.get("data")?.jsonArray?.mapNotNull {
                playlistParser.parsePlaylistToPlaylist(it.jsonObject)
            } ?: emptyList()
        )
    }

}
