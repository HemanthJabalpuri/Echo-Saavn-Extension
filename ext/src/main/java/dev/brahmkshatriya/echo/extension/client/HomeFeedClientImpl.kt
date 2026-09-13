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
                val shelves = parser.home.parseHomeFeed(response)
                shelves.toFeedData()
            } catch (e: Exception) {
                println("DEBUG: Failed to load home feed for tab ${tab?.id}: ${e.message}")
                e.printStackTrace()
                emptyList<Shelf>().toFeedData()
            }
        }
    }
}
