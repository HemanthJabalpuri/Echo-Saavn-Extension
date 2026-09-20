package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.utils.LocalLikedStore
import dev.brahmkshatriya.echo.extension.utils.Logger

class LikeClientImpl : LikeClient {

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        val settings = SaavnDependencies.settings ?: return
        if (item !is Track) return

        Logger.d("LikeClient", "likeItem: ${item.title}, shouldLike=$shouldLike")
        if (shouldLike) {
            LocalLikedStore.like(settings, item)
        } else {
            LocalLikedStore.unlike(settings, item.id)
        }
    }

    override suspend fun isItemLiked(item: EchoMediaItem): Boolean {
        val settings = SaavnDependencies.settings ?: return false
        if (item !is Track) return false
        return LocalLikedStore.isLiked(settings, item.id)
    }
}
