package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

class ShareClientImpl : ShareClient {
    override suspend fun onShare(item: EchoMediaItem): String =
        item.extras["permaUrl"] ?: "JioSaavn"
}
