package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.AlbumClient
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.utils.Logger

class AlbumClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : AlbumClient {

    private var cachedAlbumId: String? = null
    private var cachedResponse: JsonObject? = null
    private var cachedAlbum: Album? = null

    // ===== LOAD ALBUM =====
    // Fetches full details, caches response and enriched album
    override suspend fun loadAlbum(album: Album): Album {
        // Cache hit
        if (cachedAlbumId == album.id && cachedAlbum != null) {
            return cachedAlbum!!
        }

        // Fetch, parse, cache
        val response = api.album.getDetails(album.id)
        val parsedAlbum = parser.album.parseAlbumToAlbum(response) ?: album

        cachedAlbumId = album.id
        cachedResponse = response
        cachedAlbum = parsedAlbum

        return parsedAlbum
    }

    // ===== LOAD TRACKS =====
    // Requires cache from loadAlbum
    override suspend fun loadTracks(album: Album): Feed<Track>? {
        val response = cachedResponse.takeIf { cachedAlbumId == album.id }
            ?: return null

        val tracks = parser.album.parseAlbumTracks(response)
        return tracks.toFeed() as Feed<Track>
    }

    override suspend fun loadFeed(album: Album): Feed<Shelf>? {
        val response = cachedResponse.takeIf { cachedAlbumId == album.id }
            ?: return null

        val shelves = mutableListOf<Shelf>()

        // ===== ARTISTS SHELF =====
        val artistMapJson = response["more_info"]?.jsonObject
            ?.get("artistMap")?.jsonObject
            ?.toString()
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

        // ===== YOU MIGHT LIKE SHELF =====
        val rawAlbumId = response["id"]?.jsonPrimitive?.content
        if (!rawAlbumId.isNullOrBlank()) {
            try {
                val recoResponse = api.album.getAlbumReco(rawAlbumId)
                val recoAlbums = parser.album.parseAlbumReco(recoResponse)
                    .filter { it.id != album.id }

                if (recoAlbums.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "you_might_like",
                            title = "You Might Like",
                            list = recoAlbums,
                            subtitle = "${recoAlbums.size} albums"
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("AlbumClient", "Failed to load album reco", e)
            }
        }

        // ===== TRENDING ALBUMS SHELF =====
        val language_t = response["modules"]?.jsonObject
            ?.get("currentlyTrending")?.jsonObject
            ?.get("source_params")?.jsonObject
            ?.get("entity_language")?.jsonPrimitive?.content

        if (!language_t.isNullOrBlank()) {
            try {
                val trendingResponse = api.home.getTrending("album", language_t)
                val trendingAlbums = parser.album.parseTrendingAlbums(trendingResponse)
                    .filter { it.id != album.id }

                if (trendingAlbums.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "trending_albums",
                            title = "Trending Albums",
                            list = trendingAlbums,
                            subtitle = "${trendingAlbums.size} albums"
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("AlbumClient", "Failed to load trending albums", e)
            }
        }

        // ===== TOP ALBUMS FROM SAME YEAR =====
        val year = response["year"]?.jsonPrimitive?.content
        val language = response["language"]?.jsonPrimitive?.content
        if (!year.isNullOrBlank() && !language.isNullOrBlank()) {
            try {
                val yearResponse = api.album.getTopAlbumsOfYear(year, language)
                val yearAlbums = parser.album.parseTopAlbumsOfYear(yearResponse)
                    .filter { it.id != album.id }

                if (yearAlbums.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "top_albums_year",
                            title = "Top Albums of $year",
                            list = yearAlbums,
                            subtitle = "${yearAlbums.size} albums"
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.e("AlbumClient", "Failed to load top albums of year", e)
            }
        }

        return if (shelves.isEmpty()) null else shelves.toFeed()
    }

}
