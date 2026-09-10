package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import dev.brahmkshatriya.echo.extension.*

class HomeFeedClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : HomeFeedClient {

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val tabs = listOf(
            Tab(id = "hindi", title = "Hindi"),
            Tab(id = "english", title = "English"),
            Tab(id = "punjabi", title = "Punjabi"),
            Tab(id = "tamil", title = "Tamil"),
            Tab(id = "telugu", title = "Telugu"),
            Tab(id = "marathi", title = "Marathi"),
            Tab(id = "gujarati", title = "Gujarati"),
            Tab(id = "bengali", title = "Bengali"),
            Tab(id = "kannada", title = "Kannada"),
            Tab(id = "bhojpuri", title = "Bhojpuri"),
            Tab(id = "malayalam", title = "Malayalam"),
            Tab(id = "urdu", title = "Urdu")
        )
        
        return Feed(tabs) { tab ->
            try {
                val language = tab?.id ?: "hindi"
                val response = api.getHomeData(language)
                val homeData = parser.parseHomeData(response) 
                
                if (homeData == null) {
                    return@Feed emptyList<Shelf>().toFeedData()
                }
                
                val shelves = mutableListOf<Shelf>()
                if (homeData.nowTrending.isNotEmpty()) {
                    val trendingItems = homeData.nowTrending.map { mediaItem ->
                        when (mediaItem) {
                            is MediaItem.Song -> songResultToTrack(mediaItem.data)
                            is MediaItem.Album -> albumResultToAlbum(mediaItem.data)
                            is MediaItem.Playlist -> playlistResultToPlaylist(mediaItem.data)
                        }
                    }
                    shelves.add(Shelf.Lists.Items(
                        id = "now_trending",
                        title = "Now Trending",
                        list = trendingItems,
                        subtitle = "Popular content right now"
                    ))
                }
                if (homeData.topPlaylists.isNotEmpty()) {
                    shelves.add(Shelf.Lists.Items(
                        id = "top_playlists",
                        title = "Top Playlists",
                        list = homeData.topPlaylists.map { playlistResultToPlaylist(it) },
                        subtitle = "Curated playlists for you"
                    ))
                }

                if (homeData.newAlbums.isNotEmpty()) {
                    val newAlbumItems = homeData.newAlbums.map { mediaItem ->
                        when (mediaItem) {
                            is MediaItem.Album -> albumResultToAlbum(mediaItem.data)
                            is MediaItem.Song -> songResultToTrack(mediaItem.data)
                            is MediaItem.Playlist -> playlistResultToPlaylist(mediaItem.data)
                        }
                    }
                    shelves.add(Shelf.Lists.Items(
                        id = "new_albums",
                        title = "New Albums",
                        list = newAlbumItems,
                        subtitle = "Latest releases"
                    ))
                }

                if (homeData.topCharts.isNotEmpty()) {
                    shelves.add(Shelf.Lists.Items(
                        id = "top_charts",
                        title = "Top Charts",
                        list = homeData.topCharts.map { playlistResultToPlaylist(it) },
                        subtitle = "Trending charts"
                    ))
                }
                
                shelves.toFeedData()
            } catch (e: Exception) {
                println("DEBUG: Failed to load home feed for tab ${tab?.id}: ${e.message}")
                e.printStackTrace()
                emptyList<Shelf>().toFeedData()
            }
        }
    }

}
