package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import kotlinx.serialization.json.JsonObject

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.utils.Logger
import dev.brahmkshatriya.echo.extension.api.ArtistApi.ArtistCategory

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
                    more = createMoreFeed(artist, "songs")
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
                    more = createMoreFeed(artist, "albums")
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

    private fun createMoreFeed(
        artist: Artist,
        type: String
    ): Feed<Shelf> {
        val tabs = listOf(
            Tab("popular", "Popular"),
            Tab("latest", "Latest")
        )

        return Feed(tabs) { tab ->
            val category = when (tab?.id) {
                "latest" -> ArtistCategory.LATEST
                else -> ArtistCategory.POPULAR
            }

            val pagedData = buildMorePages(artist, type, category)
            pagedData.toFeedData()
        }
    }

    private fun buildMorePages(
        artist: Artist,
        type: String,
        category: ArtistCategory
    ): PagedData<Shelf> {
        Logger.d("ArtistClient", "buildMorePages: type=$type, category=$category, artist.id=${artist.id}")

        return PagedData.Continuous { continuation ->
            val page = continuation?.toIntOrNull() ?: 1
            Logger.d("ArtistClient", "buildMorePages.Continuous: page=$page, type=$type")

            try {
                val response = when (type) {
                    "songs" -> api.artist.getMoreSongs(artist.id, page, category)
                    "albums" -> api.artist.getMoreAlbums(artist.id, page, category)
                    else -> {
                        Logger.e("ArtistClient", "buildMorePages: unknown type=$type")
                        return@Continuous Page(emptyList(), null)
                    }
                }

                val items = when (type) {
                    "songs" -> parser.artist.parseArtistMoreSongs(response).map { it.toShelf() }
                    "albums" -> parser.artist.parseArtistMoreAlbums(response).map { it.toShelf() }
                    else -> emptyList()
                }

                Logger.d("ArtistClient", "buildMorePages: parsed ${items.size} items")

                if (items.isEmpty()) {
                    return@Continuous Page(emptyList(), null)
                }

                // 10 items per page
                val nextContinuation = if (items.size > 0) (page + 1).toString() else null
                Logger.d("ArtistClient", "buildMorePages: nextContinuation=$nextContinuation")
                Page(items, nextContinuation)
            } catch (e: Exception) {
                Logger.e("ArtistClient", "buildMorePages: exception", e)
                Page(emptyList(), null)
            }
        }
    }

}
