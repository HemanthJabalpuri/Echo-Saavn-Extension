package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser

class ArtistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : ArtistClient {

    // Cache fields (populated by loadArtist)
    private var cachedArtistId: String? = null
    private var cachedArtist: Artist? = null
    private var cachedTopSongs: List<Track>? = null
    private var cachedTopAlbums: List<Album>? = null
    private var cachedSingles: List<Album>? = null
    private var cachedDedicatedPlaylists: List<Playlist>? = null
    private var cachedFeaturedPlaylists: List<Playlist>? = null

    override suspend fun loadArtist(artist: Artist): Artist {
        // Cache hit
        if (cachedArtistId == artist.id && cachedArtist != null) {
            return cachedArtist!!
        }

        // Fetch 10/10
        // Artist details in Song suggestions don't have perma_url, so fallback to artistId
        val token = artist.extras["permaUrl"]?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
        val numericId = artist.extras["artistId"]?.takeIf { it.isNotBlank() }
        val response = api.artist.getDetails(token, numericId, songCount = 10, albumCount = 10, page = 1)

        // Parse and cache
        val parsedArtist = parser.artist.parseArtistToArtist(response)
            ?: throw Exception("Artist not found")
        val topSongs = parser.artist.parseArtistTopSongs(response)
        val topAlbums = parser.artist.parseArtistTopAlbums(response)

        val singles = parser.artist.parseArtistSingles(response)
        val dedicatedPlaylists = parser.artist.parseArtistDedicatedPlaylists(response)
        val featuredPlaylists = parser.artist.parseArtistFeaturedPlaylists(response)

        cachedArtistId = artist.id
        cachedArtist = parsedArtist
        cachedTopSongs = topSongs
        cachedTopAlbums = topAlbums
        cachedSingles = singles
        cachedDedicatedPlaylists = dedicatedPlaylists
        cachedFeaturedPlaylists = featuredPlaylists

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

        val singles = cachedSingles ?: emptyList()
        if (singles.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_singles",
                    title = "Singles",
                    list = singles,
                    subtitle = "${singles.size} singles"
                )
            )
        }

        val dedicatedPlaylists = cachedDedicatedPlaylists ?: emptyList()
        if (dedicatedPlaylists.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_dedicated_playlists",
                    title = "Dedicated Playlists",
                    list = dedicatedPlaylists,
                    subtitle = "${dedicatedPlaylists.size} playlists"
                )
            )
        }

        val featuredPlaylists = cachedFeaturedPlaylists ?: emptyList()
        if (featuredPlaylists.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_featured_playlists",
                    title = "Featured In",
                    list = featuredPlaylists,
                    subtitle = "${featuredPlaylists.size} playlists"
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
                        val token = artist.extras["permaUrl"]?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
                        val numericId = artist.extras["artistId"]?.takeIf { it.isNotBlank() }
                        val response = api.artist.getDetails(
                            token, numericId,
                            songCount = if (type == "songs") 10 else 0,
                            albumCount = if (type == "albums") 10 else 0,
                            page = page
                        )

                        val items = when (type) {
                            "songs" -> parser.artist.parseArtistTopSongs(response).map { it.toShelf() }
                            "albums" -> parser.artist.parseArtistTopAlbums(response).map { it.toShelf() }
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
