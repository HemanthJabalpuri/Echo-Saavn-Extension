package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.common.settings.Settings
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.utils.LocalLikedStore
import dev.brahmkshatriya.echo.extension.utils.LocalRecentStore

class LibraryFeedClientImpl : LibraryFeedClient {

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val settings = SaavnDependencies.settings
            ?: return emptyList<Shelf>().toFeed()

        val tabs = listOf(
            Tab("liked", "Liked"),
            Tab("recent", "Recently Played")
        )

        return Feed(tabs) { tab ->
            when (tab?.id) {
                "liked" -> buildLikedFeed(settings)
                "recent" -> buildRecentFeed(settings)
                else -> emptyList<Shelf>().toFeedData()
            }
        }
    }

    private fun buildLikedFeed(settings: Settings): Feed.Data<Shelf> {
        val tracks = LocalLikedStore.getAll(settings)
        return if (tracks.isEmpty()) {
            emptyList<Shelf>().toFeedData()
        } else {
            listOf(
                Shelf.Lists.Tracks(
                    id = "liked_tracks",
                    title = "Liked Songs",
                    list = tracks,
                    subtitle = "${tracks.size} tracks"
                )
            ).toFeedData()
        }
    }

    private fun buildRecentFeed(settings: Settings): Feed.Data<Shelf> {
        val tracks = LocalRecentStore.getAll(settings)
        return if (tracks.isEmpty()) {
            emptyList<Shelf>().toFeedData()
        } else {
            listOf(
                Shelf.Lists.Tracks(
                    id = "recent_tracks",
                    title = "Recently Played",
                    list = tracks,
                    subtitle = "${tracks.size} tracks"
                )
            ).toFeedData()
        }
    }
}
