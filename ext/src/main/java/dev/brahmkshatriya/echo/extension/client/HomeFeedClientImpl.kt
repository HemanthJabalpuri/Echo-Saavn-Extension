package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.extension.*
import dev.brahmkshatriya.echo.extension.utils.LANGUAGES

class HomeFeedClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : HomeFeedClient {

    override suspend fun loadHomeFeed(): Feed<Shelf> {
        val defaultLanguages = SaavnDependencies.getDefaultLanguages()
        
        val tabs = listOf(
            Tab(id = "default", title = "Default")
        ) + LANGUAGES.map { lang ->
            Tab(id = lang.lowercase(), title = lang)
        }

        return Feed(tabs) { tab ->
            try {
                val language = when (tab?.id) {
                    "default", null -> defaultLanguages.joinToString(",")
                    else -> tab.id
                }
                val response = api.home.getHomeData(language)
                val shelves = parser.home.parseHomeFeed(response)
                shelves.toFeedData()
            } catch (e: Exception) {
                emptyList<Shelf>().toFeedData()
            }
        }
    }
}
