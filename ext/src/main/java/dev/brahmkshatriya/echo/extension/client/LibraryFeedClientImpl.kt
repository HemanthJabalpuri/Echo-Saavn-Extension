package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Tab
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeedData
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.utils.LocalLikedStore

class LibraryFeedClientImpl : LibraryFeedClient {

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        val settings = SaavnDependencies.settings
            ?: return emptyList<Shelf>().toFeed()

        val tabs = listOf(
            Tab("liked", "Liked")
        )

        return Feed(tabs) { tab ->
            when (tab?.id) {
                "liked" -> {
                    val tracks = LocalLikedStore.getAll(settings)
                    if (tracks.isEmpty()) {
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
                else -> emptyList<Shelf>().toFeedData()
            }
        }
    }
}
