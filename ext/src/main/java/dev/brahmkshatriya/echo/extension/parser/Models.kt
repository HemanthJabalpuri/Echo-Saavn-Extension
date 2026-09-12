package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*

import kotlinx.serialization.json.*

// ===== STREAM =====
data class StreamUrls(
    val low: String,
    val medium: String,
    val high: String,
    val veryHigh: String
)

// ===== SEARCH =====
data class SearchAllResult(
    val songs: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<Playlist>
)

// ===== HOME =====
data class HomeData(
    val nowTrending: List<MediaItem>,
    val topPlaylists: List<Playlist>,
    val newAlbums: List<MediaItem>,
    val topCharts: List<Playlist>
)

sealed class MediaItem {
    data class Track(val data: dev.brahmkshatriya.echo.common.models.Track) : MediaItem()
    data class Album(val data: dev.brahmkshatriya.echo.common.models.Album) : MediaItem()
    data class Playlist(val data: dev.brahmkshatriya.echo.common.models.Playlist) : MediaItem()
}
