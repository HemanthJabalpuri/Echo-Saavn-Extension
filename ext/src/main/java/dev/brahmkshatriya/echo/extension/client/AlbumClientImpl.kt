package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import kotlinx.serialization.json.jsonObject

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser

class AlbumClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : AlbumClient {

    private var cachedAlbumId: String? = null
    private var cachedTracks: List<Track>? = null
    private var cachedArtistMap: String? = null

    override suspend fun loadAlbum(album: Album): Album {
        return album
    }

    override suspend fun loadTracks(album: Album): Feed<Track>? {
        if (cachedAlbumId == album.id && cachedTracks != null) {
            return cachedTracks!!.toFeed() as Feed<Track>
        }

        val response = api.album.getDetails(album.id)
        val tracks = parser.album.parseAlbumTracks(response)

        val artistMap = response["more_info"]?.jsonObject?.get("artistMap")?.jsonObject

        cachedAlbumId = album.id
        cachedTracks = tracks
        cachedArtistMap = artistMap?.toString()

        return tracks.toFeed() as Feed<Track>
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        val shelves = mutableListOf<Shelf>()

        // Prefer cached artistMap, fallback to extras
        val artistMapJson = if (cachedAlbumId == album.id) {
            cachedArtistMap
        } else {
            album.extras["artistMapJson"]
        }
        
        val allArtists = parser.artist.parseAllArtistsFromExtras(artistMapJson)
        
        if (allArtists.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "all_artists",
                    title = "Artists",
                    list = allArtists,
                    subtitle = "${allArtists.size} artists"
                )
            )
        }

        return if (shelves.isEmpty()) null else shelves.toFeed()
    }
}

