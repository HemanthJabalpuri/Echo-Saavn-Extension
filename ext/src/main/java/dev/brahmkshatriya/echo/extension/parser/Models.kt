package dev.brahmkshatriya.echo.extension.parser

import dev.brahmkshatriya.echo.common.models.*

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
    val nowTrending: List<EchoMediaItem>,
    val topPlaylists: List<Playlist>,
    val newAlbums: List<EchoMediaItem>,
    val topCharts: List<Playlist>
)
