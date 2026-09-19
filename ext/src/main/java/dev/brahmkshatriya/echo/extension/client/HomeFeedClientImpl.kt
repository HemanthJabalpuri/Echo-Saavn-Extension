package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.helpers.Page
import dev.brahmkshatriya.echo.common.helpers.PagedData
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.parser.HomeSection
import dev.brahmkshatriya.echo.extension.parser.MoreInfo
import dev.brahmkshatriya.echo.extension.utils.LANGUAGES
import dev.brahmkshatriya.echo.extension.utils.Logger

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
                val sections = parser.home.parseHomeSections(response)
                val shelves = sections.map { section -> buildShelf(section) }
                
                shelves.toFeedData()
            } catch (e: Exception) {
                Logger.e("HomeClient", "Failed to load home feed", e)
                emptyList<Shelf>().toFeedData()
            }
        }
    }

    private fun buildShelf(section: HomeSection): Shelf {
        val more = section.moreInfo?.let { info ->
            createMoreFeed(info)
        }
        
        return Shelf.Lists.Items(
            id = section.id,
            title = section.title,
            list = section.items,
            subtitle = section.subtitle,
            more = more
        )
    }

    private fun createMoreFeed(info: MoreInfo): Feed<Shelf> {
        return Feed(emptyList()) { _ ->
            Feed.Data(
                PagedData.Continuous<Shelf> { continuation ->
                    val page = continuation?.toIntOrNull() ?: 1
                    try {
                        val response = api.home.getMore(
                            api = info.api,
                            page = page,
                            size = info.defaultSize,
                            pageParam = info.pageParam,
                            sizeParam = info.sizeParam
                        )
                        val items = parser.home.parseMoreResponse(response)
                        val nextContinuation = if (items.size > 0) (page + 1).toString() else null
                        Page(items, nextContinuation)
                    } catch (e: Exception) {
                        Logger.e("HomeClient", "Failed to load more for ${info.api}", e)
                        Page(emptyList(), null)
                    }
                }
            )
        }
    }

}
