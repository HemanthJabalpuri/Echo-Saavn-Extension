package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.*

class ShareClientImpl : ShareClient {
    override suspend fun onShare(item: EchoMediaItem): String {
        return when (item) {
            is Track -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/song/${item.id}"
            is Album -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/album/${item.id}"
            is Artist -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/artist/${item.id}"
            is Playlist -> item.extras["permaUrl"] ?: "https://www.jiosaavn.com/featured/${item.id}"
            else -> throw Exception("Sharing not supported for this item type")
        }
    }
}
