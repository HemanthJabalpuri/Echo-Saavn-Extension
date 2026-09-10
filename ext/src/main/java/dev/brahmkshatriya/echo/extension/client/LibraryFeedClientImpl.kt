package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LibraryFeedClient
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

class LibraryFeedClientImpl : LibraryFeedClient {

    override suspend fun loadLibraryFeed(): Feed<Shelf> {
        println("DEBUG: loadLibraryFeed() called - Creating library feed")
        
        return try {
            val shelves = mutableListOf<Shelf>()
            
            val favoritesCategory = Shelf.Category(
                id = "favorites",
                title = "Favorites",
                subtitle = "Your favorite music",
                feed = null,
                extras = mapOf("type" to "favorites")
            )
            shelves.add(favoritesCategory)
            
            val recentlyPlayedCategory = Shelf.Category(
                id = "recently_played",
                title = "Recently Played",
                subtitle = "Your recently played tracks",
                feed = null,
                extras = mapOf("type" to "recently_played")
            )
            shelves.add(recentlyPlayedCategory)
            
            val playlistsCategory = Shelf.Category(
                id = "playlists",
                title = "My Playlists",
                subtitle = "Your created playlists",
                feed = null,
                extras = mapOf("type" to "playlists")
            )
            shelves.add(playlistsCategory)
            
            println("DEBUG: Created library feed with ${shelves.size} category shelves")
            shelves.toFeed()
            
        } catch (e: Exception) {
            println("DEBUG: Error in loadLibraryFeed: ${e.message}")
            e.printStackTrace()
            
            val errorShelf = Shelf.Category(
                id = "library_error",
                title = "Library",
                subtitle = "Unable to load library content",
                feed = null
            )
            listOf(errorShelf).toFeed()
        }
    }
}
