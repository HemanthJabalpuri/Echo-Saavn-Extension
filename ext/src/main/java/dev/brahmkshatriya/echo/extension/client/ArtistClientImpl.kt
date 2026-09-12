package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import kotlinx.serialization.json.*

class ArtistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : ArtistClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Cache fields (populated by loadArtist)
    private var cachedArtistId: String? = null
    private var cachedArtist: Artist? = null
    private var cachedTopSongs: List<Track>? = null
    private var cachedTopAlbums: List<Album>? = null

    override suspend fun loadArtist(artist: Artist): Artist {
        // Cache hit
        if (cachedArtistId == artist.id && cachedArtist != null) {
            return cachedArtist!!
        }

        // Fetch 10/10
        val response = api.getArtistDetails(artist.id, songCount = 10, albumCount = 10, page = 1)
        val jsonObject = json.parseToJsonElement(response).jsonObject

        // Parse and cache
        val parsedArtist = parser.parseArtistToArtist(jsonObject)
            ?: throw Exception("Artist not found")
        val topSongs = parser.parseArtistTopSongs(jsonObject)
        val topAlbums = parser.parseArtistTopAlbums(jsonObject)

        cachedArtistId = artist.id
        cachedArtist = parsedArtist
        cachedTopSongs = topSongs
        cachedTopAlbums = topAlbums

        return parsedArtist
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        // Cache hit - build shelves from cached data
        if (cachedArtistId == artist.id && cachedTopSongs != null) {
            return buildFeed(artist)
        }

        // Cache miss - unexpected flow, return empty
        return emptyList<Shelf>().toFeed()
    }

    private fun buildFeed(artist: Artist): Feed<Shelf> {
        val shelves = mutableListOf<Shelf>()

        val topSongs = cachedTopSongs ?: emptyList()
        if (topSongs.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_songs",
                    title = "Top Songs",
                    list = topSongs,
                    subtitle = "${topSongs.size} songs",
                    more = createMoreFeed(artist, "songs")
                )
            )
        }

        val topAlbums = cachedTopAlbums ?: emptyList()
        if (topAlbums.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_albums",
                    title = "Top Albums",
                    list = topAlbums,
                    subtitle = "${topAlbums.size} albums",
                    more = createMoreFeed(artist, "albums")
                )
            )
        }

        return shelves.toFeed()
    }

    private fun createMoreFeed(artist: Artist, type: String): Feed<Shelf> {
        return Feed(emptyList()) { _ ->
            Feed.Data(
                PagedData.Continuous<Shelf> { continuation ->
                    val page = continuation?.toIntOrNull() ?: 2
                    try {
                        val response = api.getArtistDetails(
                            artist.id,
                            songCount = if (type == "songs") 10 else 0,
                            albumCount = if (type == "albums") 10 else 0,
                            page = page
                        )
                        val jsonObject = json.parseToJsonElement(response).jsonObject

                        val items = when (type) {
                            "songs" -> parser.parseArtistTopSongs(jsonObject).map { it.toShelf() }
                            "albums" -> parser.parseArtistTopAlbums(jsonObject).map { it.toShelf() }
                            else -> emptyList()
                        }

                        if (items.isEmpty()) {
                            return@Continuous Page(emptyList(), null)
                        }

                        val nextContinuation = if (items.size >= 10) (page + 1).toString() else null
                        Page(items, nextContinuation)
                    } catch (e: Exception) {
                        Page(emptyList(), null)
                    }
                }
            )
        }
    }
}
