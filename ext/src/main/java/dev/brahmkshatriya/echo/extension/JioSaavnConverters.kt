package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.JioSaavnParser.*
import dev.brahmkshatriya.echo.extension.utils.*

import dev.brahmkshatriya.echo.extension.parser.*

fun albumResultToAlbum(album: AlbumResult): Album {
    // Build artist list with IDs
    val artistNames = album.primaryArtists.split(", ").filter { it.isNotBlank() }
    val artistIds = album.primaryArtistsId.split(", ").filter { it.isNotBlank() }
    
    val artists = artistNames.mapIndexed { index, name ->
        val id = if (index < artistIds.size) artistIds[index] else ""
        Artist(
            id = id,
            name = name,
            cover = null,
            bio = null,
            background = null,
            banners = emptyList(),
            subtitle = null,
            extras = emptyMap()
        )
    }
    
    return Album(
        id = album.id,
        title = album.title,
        type = null,
        cover = convertImageUrl(album.image).toImageHolder(),
        artists = artists,
        trackCount = album.songCount.toLongOrNull(),
        duration = null,
        releaseDate = null,
        description = null,
        background = convertImageUrl(album.image).toImageHolder(),
        label = null,
        isExplicit = album.explicitContent,
        subtitle = album.subtitle,
        extras = mapOf(
            "language" to album.language,
            "year" to album.year,
            "permaUrl" to album.permaUrl,
            "type" to album.type
        )
    )
}

fun albumDetailToAlbum(album: AlbumDetail): Album {
    // Build artist list with IDs
    val artistNames = album.primaryArtists.split(", ").filter { it.isNotBlank() }
    val artistIds = album.primaryArtistsId.split(", ").filter { it.isNotBlank() }
    
    val artists = artistNames.mapIndexed { index, name ->
        val id = if (index < artistIds.size) artistIds[index] else ""
        Artist(
            id = id,
            name = name,
            cover = null,
            bio = null,
            background = null,
            banners = emptyList(),
            subtitle = null,
            extras = emptyMap()
        )
    }
    
    return Album(
        id = album.id,
        title = album.title,
        type = null,
        cover = convertImageUrl(album.image).toImageHolder(),
        artists = artists,
        trackCount = album.songCount.toLongOrNull(),
        duration = null,
        releaseDate = parseDate(album.releaseDate),
        description = null,
        background = convertImageUrl(album.image).toImageHolder(),
        label = null,
        isExplicit = album.explicitContent,
        subtitle = album.subtitle,
        extras = mapOf(
            "language" to album.language,
            "year" to album.year,
            "permaUrl" to album.permaUrl,
            "type" to album.type
        )
    )
}

fun artistResultToArtist(artist: ArtistResult): Artist {
    return Artist(
        id = artist.id,
        name = artist.name,
        cover = convertImageUrl(artist.image).toImageHolder(),
        bio = null,
        background = convertImageUrl(artist.image).toImageHolder(),
        banners = emptyList(),
        subtitle = artist.role,
        extras = mapOf(
            "permaUrl" to artist.permaUrl,
            "type" to artist.type,
            "role" to artist.role
        )
    )
}

fun artistDetailToArtist(artist: ArtistDetail): Artist {
    return Artist(
        id = artist.id,
        name = artist.name,
        cover = convertImageUrl(artist.image).toImageHolder(),
        bio = null,
        background = convertImageUrl(artist.image).toImageHolder(),
        banners = emptyList(),
        subtitle = artist.subtitle,
        extras = mapOf(
            "followerCount" to artist.followerCount,
            "type" to artist.type,
            "isVerified" to artist.isVerified.toString(),
            "dominantLanguage" to artist.dominantLanguage,
            "dominantType" to artist.dominantType,
            "isRadioPresent" to artist.isRadioPresent.toString() // Add this
        )
    )
}

fun playlistResultToPlaylist(playlist: PlaylistResult): Playlist {
    return Playlist(
        id = playlist.id,
        title = playlist.title,
        isEditable = false,
        isPrivate = false,
        cover = convertImageUrl(playlist.image).toImageHolder(),
        authors = emptyList(),
        trackCount = playlist.songCount.toLongOrNull(),
        duration = null,
        creationDate = null,
        description = null,
        background = convertImageUrl(playlist.image).toImageHolder(),
        subtitle = playlist.subtitle,
        extras = mapOf(
            "language" to playlist.language,
            "permaUrl" to playlist.permaUrl,
            "type" to playlist.type
        )
    )
}

fun playlistDetailToPlaylist(playlist: PlaylistDetail): Playlist {
    return Playlist(
        id = playlist.id,
        title = playlist.title,
        isEditable = false,
        isPrivate = false,
        cover = convertImageUrl(playlist.image).toImageHolder(),
        authors = emptyList(),
        trackCount = playlist.songCount.toLongOrNull(),
        duration = null,
        creationDate = null,
        description = null,
        background = convertImageUrl(playlist.image).toImageHolder(),
        subtitle = playlist.subtitle,
        extras = mapOf(
            "language" to playlist.language,
            "permaUrl" to playlist.permaUrl,
            "type" to playlist.type,
            "followerCount" to playlist.followerCount
        )
    )
}
