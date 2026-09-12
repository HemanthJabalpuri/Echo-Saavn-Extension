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
    val artists: List<ArtistResult>,
    val playlists: List<Playlist>
)

// ===== ARTIST =====
data class ArtistResult(
    val id: String,
    val name: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val role: String
)

data class ArtistDetail(
    val id: String,
    val name: String,
    val subtitle: String,
    val image: String,
    val followerCount: String,
    val type: String,
    val isVerified: Boolean,
    val dominantLanguage: String,
    val dominantType: String,
    val topSongs: List<Track>,
    val topAlbums: List<Album>,
    val isRadioPresent: Boolean = false
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
