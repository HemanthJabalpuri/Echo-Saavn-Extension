package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.*

import kotlinx.serialization.json.*

class AlbumClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : AlbumClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // One-album cache
    private var cachedAlbumId: String? = null
    private var cachedAlbum: Album? = null
    private var cachedTracks: List<Track>? = null

    override suspend fun loadAlbum(album: Album): Album {
        // Cache hit
        if (cachedAlbumId == album.id && cachedAlbum != null) {
            return cachedAlbum!!
        }

        // Cache miss - fetch, parse, cache
        val response = api.getAlbumDetails(album.id)
        val jsonObject = json.parseToJsonElement(response).jsonObject
        val parsedAlbum = parser.album.parseAlbumToAlbum(jsonObject)
            ?: throw Exception("Album not found")
        val tracks = parser.album.parseAlbumTracks(jsonObject)

        cachedAlbumId = album.id
        cachedAlbum = parsedAlbum
        cachedTracks = tracks

        return parsedAlbum
    }

    override suspend fun loadTracks(album: Album): Feed<Track>? {
        // Cache hit
        if (cachedAlbumId == album.id && cachedTracks != null) {
            return cachedTracks!!.toFeed() as Feed<Track>
        }

        // Cache miss - unexpected flow, return empty
        return emptyList<Track>().toFeed() as Feed<Track>
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? = null
}
