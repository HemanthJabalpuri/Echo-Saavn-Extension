package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ArtistClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import dev.brahmkshatriya.echo.extension.*

class ArtistClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : ArtistClient {

    override suspend fun loadArtist(artist: Artist): Artist {
        return try {            
            println("DEBUG: Loading artist with ID: ${artist.id}")
            // Fetch only 10 songs for the initial metadata
            val response = api.getArtistDetails(artist.id, songCount = 10, albumCount = 10, page = 1)
            val artistDetail = parser.parseArtistDetails(response)
                ?: throw Exception("Artist not found")
            
            artistDetailToArtist(artistDetail)
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist ${artist.id}: ${e.message}")
            throw Exception("Failed to load artist: ${e.message}")
        }
    }

    override suspend fun loadFeed(artist: Artist): Feed<Shelf> {
        return try {
            // Fetch first page of songs and albums
            val initialResponse = api.getArtistDetails(artist.id, songCount = 50, albumCount = 50, page = 1)
            val artistDetail = parser.parseArtistDetails(initialResponse)
                ?: return emptyList<Shelf>().toFeed()

            val shelves = mutableListOf<Shelf>()

            // Songs shelf with "More" button
            val firstPageSongs = artistDetail.topSongs
            if (firstPageSongs.isNotEmpty()) {
                shelves.add(
                    Shelf.Lists.Items(
                        id = "artist_songs",
                        title = "Top Songs",
                        list = firstPageSongs,
                        subtitle = "${firstPageSongs.size} songs",
                        more = if (firstPageSongs.size >= 50) createMoreFeed(artist, "songs") else null
                    )
                )
            }

            // Albums shelf with "More" button
            val firstPageAlbums = artistDetail.topAlbums
            if (firstPageAlbums.isNotEmpty()) {
                shelves.add(
                    Shelf.Lists.Items(
                        id = "artist_albums",
                        title = "Top Albums",
                        list = firstPageAlbums,
                        subtitle = "${firstPageAlbums.size} albums",
                        more = if (firstPageAlbums.size >= 50) createMoreFeed(artist, "albums") else null
                    )
                )
            }

            shelves.toFeed()
        } catch (e: Exception) {
            println("DEBUG: Failed to load artist feed: ${e.message}")
            emptyList<Shelf>().toFeed()
        }
    }

    private fun createMoreFeed(artist: Artist, type: String): Feed<Shelf> {
        return Feed(emptyList()) { _ ->
            Feed.Data(
                PagedData.Continuous<Shelf> { continuation ->
                    val page = continuation?.toIntOrNull() ?: 1
                    
                    try {
                        val response = api.getArtistDetails(
                            artist.id,
                            songCount = if (type == "songs") 50 else 0,
                            albumCount = if (type == "albums") 50 else 0,
                            page = page
                        )
                        val detail = parser.parseArtistDetails(response)
                            ?: return@Continuous Page(emptyList(), null)
                        
                        val items = when (type) {
                            "songs" -> detail.topSongs.map { it.toShelf() }
                            "albums" -> detail.topAlbums.map { it.toShelf() }
                            else -> emptyList()
                        }
                        
                        // If we got exactly 50 items, assume there's a next page
                        val nextContinuation = if (items.size >= 50) {
                            (page + 1).toString()
                        } else {
                            null
                        }
                        
                        Page(items, nextContinuation)
                        
                    } catch (e: Exception) {
                        Page(emptyList(), null)
                    }
                }
            )
        }
    }

}
