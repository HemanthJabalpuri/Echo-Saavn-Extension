package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track

import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.utils.Extras

class ShareClientImpl : ShareClient {

    override suspend fun onShare(item: EchoMediaItem): String {
        val url = item.extras[Extras.PERMA_URL].orEmpty()

        if (!SaavnDependencies.isRichShareEnabled()) {
            return url.ifBlank { "JioSaavn" }
        }

        // Rich mode — currently only tracks get expanded.
        // Other types fall back to URL.
        return when (item) {
            is Track -> formatTrack(item, url)
            is Album -> formatAlbum(item, url)
            else -> url.ifBlank { "JioSaavn" }
        }
    }

    private fun formatTrack(track: Track, url: String): String = buildString {
        append(track.title)
        val primaryArtist = track.artists.firstOrNull()?.name
        if (!primaryArtist.isNullOrBlank()) {
            append(" - ").append(primaryArtist)
        }
        track.album?.title?.takeIf { it.isNotBlank() }?.let {
            append("\nAlbum: ").append(it)
        }
        if (url.isNotBlank()) {
            append("\n").append(url)
        }
    }

    private fun formatAlbum(album: Album, url: String): String = buildString {
        append(album.title)
        album.label?.takeIf { it.isNotBlank() }?.let {
            append("\nLabel: ").append(it)
        }
        if (url.isNotBlank()) {
            append("\n").append(url)
        }
    }
}
