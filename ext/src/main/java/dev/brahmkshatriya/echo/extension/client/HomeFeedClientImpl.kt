package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.extension.*

class HomeFeedClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : HomeFeedClient {

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val defaultLanguages = SaavnDependencies.getDefaultLanguages()
        
        val tabs = listOf(
            Tab(id = "default", title = "Default"),
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
                val language = when (tab?.id) {
                    "default", null -> defaultLanguages.joinToString(",")  // "hindi,telugu"
                    else -> tab.id
                }
                val response = api.getHomeData(language)
                val shelves = parser.home.parseHomeFeed(response)
                shelves.toFeedData()
            } catch (e: Exception) {
                emptyList<Shelf>().toFeedData()
            }
        }
    }
}
