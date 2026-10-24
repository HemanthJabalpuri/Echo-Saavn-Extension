package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.ShareClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.service.AlbumService
import dev.brahmkshatriya.echo.extension.storage.LocalPlaylistTracksStore
import dev.brahmkshatriya.echo.extension.utils.Extras
import dev.brahmkshatriya.echo.extension.utils.Logger
import dev.brahmkshatriya.echo.extension.utils.Prefixes
import dev.brahmkshatriya.echo.extension.utils.getToken

class ShareClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser,
    private val albumService: AlbumService,
) : ShareClient {

    override suspend fun onShare(item: EchoMediaItem): String {
        val url = item.extras[Extras.PERMA_URL].orEmpty()

        if (!SaavnDependencies.isRichShareEnabled()) {
            return url.ifBlank { "JioSaavn" }
        }

        return when (item) {
            is Track -> formatTrack(item, url)
            is Album -> formatAlbum(item, url, albumService.getCachedTracks(item.id))
            is Playlist -> {
                val tracks = if (Prefixes.isLocal(item.id)) {
                    SaavnDependencies.settings
                        ?.let { LocalPlaylistTracksStore.getTracks(it, item.id) }
                        ?: emptyList()
                } else {
                    fetchAllPlaylistTracks(item)
                }
                formatPlaylist(item, url, tracks)
            }
            else -> url.ifBlank { "JioSaavn" }
        }
    }

    // ===== TRACK =====

    private fun formatTrack(track: Track, url: String): String = buildString {
        append(track.title)
        track.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let {
            append(" — ").append(it)
        }
        track.album?.title?.takeIf { it.isNotBlank() }?.let {
            append("\nAlbum: ").append(it)
        }
        if (url.isNotBlank()) append("\n").append(url)
    }

    // ===== ALBUM (cache-only) =====

    private fun formatAlbum(
        album: Album,
        url: String,
        tracks: List<Track>?,
    ): String = buildString {
        append(album.title)
        album.label?.takeIf { it.isNotBlank() }?.let {
            append("\nLabel: ").append(it)
        }
        if (!tracks.isNullOrEmpty()) {
            append("\n")
            tracks.forEachIndexed { i, t ->
                append("\n${i + 1}. ").append(t.title)
                t.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let {
                    append(" — ").append(it)
                }
            }
        }
        if (url.isNotBlank()) append("\n\n").append(url)
    }

    // ===== PLAYLIST (fetch-on-share or local) =====

    private fun formatPlaylist(
        playlist: Playlist,
        url: String,
        tracks: List<Track>,
    ): String = buildString {
        append(playlist.title)
        buildPlaylistSubtitle(playlist, tracks.size)?.let {
            append("\n").append(it)
        }

        if (tracks.isNotEmpty()) {
            append("\n")
            tracks.forEachIndexed { i, t ->
                append("\n${i + 1}. ").append(t.title)
                t.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() }?.let {
                    append(" — ").append(it)
                }
            }
        }

        if (url.isNotBlank()) append("\n\n").append(url)
    }

    private fun buildPlaylistSubtitle(playlist: Playlist, fetchedCount: Int): String? {
        val parts = mutableListOf<String>()
        playlist.authors.firstOrNull()?.name?.takeIf { it.isNotBlank() }
            ?.let { parts.add("By $it") }
        playlist.trackCount?.takeIf { it > 0 }?.let { expected ->
            parts.add(
                if (fetchedCount >= expected) "$expected tracks"
                else "$fetchedCount of $expected tracks"
            )
        }
        return parts.joinToString(" · ").ifBlank { null }
    }

    // ===== Playlist paginated fetch =====

    private suspend fun fetchAllPlaylistTracks(playlist: Playlist): List<Track> {
        val token = playlist.getToken()
        if (token.isBlank()) return emptyList()

        val tracks = mutableListOf<Track>()
        val expectedTotal = playlist.trackCount?.toInt()
        var page = 1

        try {
            while (true) {
                val response = api.playlist.getDetails(token, page = page, count = PAGE_SIZE)
                val pageTracks = parser.playlist.parsePlaylistTracks(response)
                if (pageTracks.isEmpty()) break
                tracks.addAll(pageTracks)

                if (pageTracks.size < PAGE_SIZE) break
                if (expectedTotal != null && tracks.size >= expectedTotal) break
                page++
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ShareClient", "Failed to fetch full playlist ${playlist.id}", e)
        }

        return tracks
    }

    companion object {
        private const val PAGE_SIZE = 100
    }
}
