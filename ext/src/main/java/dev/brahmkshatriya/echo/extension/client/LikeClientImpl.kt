package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.extension.utils.Logger

class LikeClientImpl : LikeClient {

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        Logger.d("LikeClient", "likeItem (not implemented): ${item::class.simpleName} id=${item.id} like=$shouldLike")
    }

    override suspend fun isItemLiked(item: EchoMediaItem): Boolean {
        Logger.d("LikeClient", "isItemLiked (not implemented): ${item::class.simpleName} id=${item.id}")
        return false
    }
}
