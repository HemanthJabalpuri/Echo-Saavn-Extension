package dev.brahmkshatriya.echo.extension.parser

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
    val songs: List<SongResult>,
    val albums: List<AlbumResult>,
    val artists: List<ArtistResult>,
    val playlists: List<PlaylistResult>
)

// ===== SONG =====
data class SongResult(
    val id: String,
    val numericId: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val playCount: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val albumId: String?,
    val album: String,
    val duration: String
)

data class SongDetail(
    val id: String,
    val numericId: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val playCount: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val primaryArtistsId: String,
    val featuredArtists: String?,
    val albumId: String?,
    val album: String,
    val albumUrl: String?,
    val duration: String,
    val label: String,
    val copyright: String,
    val releaseDate: String?,
    val hasLyrics: Boolean,
    val lyricsId: String?,
    val encryptedMediaUrl: String?,
    val streamUrls: StreamUrls?,
    val is320kbps: Boolean
)

// ===== ALBUM =====
data class AlbumResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val explicitContent: Boolean,
    val songCount: String,
    val primaryArtists: String = "",
    val primaryArtistsId: String = ""
)

data class AlbumDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val primaryArtistsId: String,
    val songCount: String,
    val releaseDate: String?,
    val songs: List<SongDetail>
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
    val topSongs: List<SongDetail>,
    val topAlbums: List<AlbumResult>,
    val isRadioPresent: Boolean = false
)

// ===== PLAYLIST =====
data class PlaylistResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val explicitContent: Boolean,
    val songCount: String
)

data class PlaylistDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val explicitContent: Boolean,
    val songCount: String,
    val followerCount: String,
    val songs: List<SongDetail>
)

// ===== HOME =====
data class HomeData(
    val nowTrending: List<MediaItem>,
    val topPlaylists: List<PlaylistResult>,
    val newAlbums: List<MediaItem>,
    val topCharts: List<PlaylistResult>
)

sealed class MediaItem {
    data class Song(val data: SongResult) : MediaItem()
    data class Album(val data: AlbumResult) : MediaItem()
    data class Playlist(val data: PlaylistResult) : MediaItem()
}
