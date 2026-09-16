package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import kotlinx.serialization.json.JsonObject

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.utils.Logger

class ArtistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : ArtistClient {

    private var cachedArtistId: String? = null
    private var cachedFeed: Feed<Shelf>? = null

    // ===== LOAD ARTIST =====
    // No API call - return as-is
    override suspend fun loadArtist(artist: Artist): Artist {
        return artist
    }

    // ===== LOAD FEED =====
    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        // Cache hit
        if (cachedArtistId == artist.id && cachedFeed != null) {
            return cachedFeed!!
        }

        // Try to get token from extras
        var resolvedArtist = artist
        var token = artist.extras["permaUrl"]?.substringAfterLast("/")?.takeIf { it.isNotBlank() }

        // If token missing, search by name and match by artist.id
        // In song suggestions, perma_url is empty, that's why
        if (token.isNullOrBlank()) {
            Logger.d("ArtistClient", "Token missing for ${artist.name} (id=${artist.id}), searching by name")
            val matchedArtist = findArtistByName(artist.name, artist.id)

            if (matchedArtist == null) {
                Logger.e("ArtistClient", "Could not find artist: ${artist.name} (id=${artist.id})")
                return emptyList<Shelf>().toFeed()
            }

            // Use the matched artist (has permaUrl in extras)
            resolvedArtist = matchedArtist
            // Get token from permaUrl
            token = resolvedArtist.extras["permaUrl"]?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
                ?: run {
                    Logger.e("ArtistClient", "No token in permaUrl for: ${resolvedArtist.name}")
                    return emptyList<Shelf>().toFeed()
                }

            Logger.d("ArtistClient", "Resolved artist: ${matchedArtist.name} (token=$token)")
        }

        // Fetch details using token
        val response = api.artist.getDetails(
            token = token,
        )

        val feed = buildFeed(resolvedArtist, response)
        cachedArtistId = artist.id  // Cache by original ID
        cachedFeed = feed
        return feed
    }

    private suspend fun findArtistByName(name: String, numericId: String): Artist? {
        return try {
            val response = api.artist.search(name, page = 1, limit = 10)
            val artists = parser.artist.parseArtistSearchResults(response)

            // Match by numeric ID (either in id or extras)
            artists.firstOrNull { it.id == numericId }
        } catch (e: Exception) {
            Logger.e("ArtistClient", "Search failed for $name", e)
            null
        }
    }

    private fun buildFeed(artist: Artist, response: JsonObject): Feed<Shelf> {
        val shelves = mutableListOf<Shelf>()

        // Top Songs
        val topSongs = parser.artist.parseArtistTopSongs(response)
        if (topSongs.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_songs",
                    title = "Top Songs",
                    list = topSongs,
                    subtitle = "${topSongs.size} songs",
                    more = createMoreFeed(artist, "songs", topSongs)
                )
            )
        }

        // Top Albums
        val topAlbums = parser.artist.parseArtistTopAlbums(response)
        if (topAlbums.isNotEmpty()) {
            shelves.add(
                Shelf.Lists.Items(
                    id = "artist_albums",
                    title = "Top Albums",
                    list = topAlbums,
                    subtitle = "${topAlbums.size} albums",
                    more = createMoreFeed(artist, "albums", topAlbums)
                )
            )
        }

        // Singles
        val singles = parser.artist.parseArtistSingles(response)
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

        // Dedicated Playlists
        val dedicatedPlaylists = parser.artist.parseArtistDedicatedPlaylists(response)
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

        // Featured Playlists
        val featuredPlaylists = parser.artist.parseArtistFeaturedPlaylists(response)
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

    // ===== MORE FEED =====
    private fun createMoreFeed(
        artist: Artist,
        type: String,
        firstPage: List<EchoMediaItem>
    ): Feed<Shelf> {
        // Convert to Shelf for consistency
        val firstPageShelves = firstPage.map { Shelf.Item(it) }

        val morePages = PagedData.Continuous<Shelf> { continuation ->
            val page = continuation?.toIntOrNull() ?: 2
            try {
                val token = artist.extras["permaUrl"]?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
                val response = api.artist.getDetails(
                    token = token!!,
                    page = page,
                    subType = type,
                )

                val items = when (type) {
                    "songs" -> parser.artist.parseArtistTopSongs(response).map { it.toShelf() }
                    "albums" -> parser.artist.parseArtistTopAlbums(response).map { it.toShelf() }
                    else -> emptyList()
                }

                if (items.isEmpty()) {
                    return@Continuous Page(emptyList(), null)
                }

                val nextContinuation = if (items.size >= 50) (page + 1).toString() else null
                Page(items, nextContinuation)
            } catch (e: Exception) {
                Page(emptyList(), null)
            }
        }

        val combined = PagedData.Concat<Shelf>(
            PagedData.Single { firstPageShelves },
            morePages
        )

        return Feed(emptyList()) { _ ->
            Feed.Data(combined)
        }
    }
}
