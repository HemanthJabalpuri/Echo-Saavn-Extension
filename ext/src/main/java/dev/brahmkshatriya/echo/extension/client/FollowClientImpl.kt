package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.FollowClient
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.utils.Logger

class FollowClientImpl(
    private val api: JioSaavnApi,
) : FollowClient {

    override suspend fun isFollowing(item: EchoMediaItem): Boolean {
        if (item !is Artist) return false
        return try {
            val library = api.library.getAll()
            val followed = library["artist"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList()
            item.id in followed
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("FollowClient", "Failed to check follow status for ${item.id}", e)
            false
        }
    }

    override suspend fun getFollowersCount(item: EchoMediaItem): Long? = null

    override suspend fun followItem(item: EchoMediaItem, shouldFollow: Boolean) {
        if (item !is Artist) return
        try {
            if (shouldFollow) {
                api.follow.follow(item.id)
            } else {
                api.follow.unfollow(item.id)
            }
            Logger.d("FollowClient", "${if (shouldFollow) "Followed" else "Unfollowed"} ${item.id}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("FollowClient", "Failed to ${if (shouldFollow) "follow" else "unfollow"} ${item.id}", e)
        }
    }
}
