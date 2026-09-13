package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser

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
                val homeData = parser.home.parseHomeData(response)

                if (homeData == null) {
                    return@Feed emptyList<Shelf>().toFeedData()
                }

                val shelves = mutableListOf<Shelf>()

                // Now Trending
                if (homeData.nowTrending.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "now_trending",
                            title = "Now Trending",
                            list = homeData.nowTrending,
                            subtitle = "Popular content right now"
                        )
                    )
                }

                // Top Playlists
                if (homeData.topPlaylists.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "top_playlists",
                            title = "Top Playlists",
                            list = homeData.topPlaylists,
                            subtitle = "Curated playlists for you"
                        )
                    )
                }

                // New Albums
                if (homeData.newAlbums.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "new_albums",
                            title = "New Albums",
                            list = homeData.newAlbums,
                            subtitle = "Latest releases"
                        )
                    )
                }

                // Top Charts
                if (homeData.topCharts.isNotEmpty()) {
                    shelves.add(
                        Shelf.Lists.Items(
                            id = "top_charts",
                            title = "Top Charts",
                            list = homeData.topCharts,
                            subtitle = "Trending charts"
                        )
                    )
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
