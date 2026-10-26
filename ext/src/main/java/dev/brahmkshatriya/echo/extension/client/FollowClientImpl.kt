package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.utils.Logger

class FollowClientImpl(
    private val api: JioSaavnApi,
) : FollowClient {

    override suspend fun isFollowing(item: EchoMediaItem): Boolean {
        val type = when (item) {
            is Artist -> "artist"
            is Playlist -> "playlist"
            else -> return false
        }
        return try {
            val library = api.library.getAll()
            val entries = library[type]?.jsonArray ?: return false
            when (type) {
                "artist" -> entries.any { it.jsonPrimitive.contentOrNull == item.id }
                "playlist" -> entries.any {
                    it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == item.id
                }
                else -> false
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("FollowClient", "Failed to check follow status for $type/${item.id}", e)
            false
        }
    }

    override suspend fun getFollowersCount(item: EchoMediaItem): Long? = null

    override suspend fun followItem(item: EchoMediaItem, shouldFollow: Boolean) {
        val (id, type) = when (item) {
            is Artist -> item.id to "artist"
            is Playlist -> item.id to "playlist"
            else -> return
        }
        try {
            if (shouldFollow) api.follow.follow(id, type)
            else api.follow.unfollow(id, type)
            Logger.d("FollowClient", "${if (shouldFollow) "Followed" else "Unfollowed"} $type/$id")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("FollowClient", "Failed to ${if (shouldFollow) "follow" else "unfollow"} $type/$id", e)
        }
    }
}
