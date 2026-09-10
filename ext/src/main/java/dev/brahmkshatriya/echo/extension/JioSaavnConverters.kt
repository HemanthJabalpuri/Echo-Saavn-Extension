package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.JioSaavnParser.*
import dev.brahmkshatriya.echo.extension.utils.*

fun songResultToTrack(song: SongResult): Track {
    return Track(
        id = song.id,
        title = song.title,
        type = Track.Type.Song,
        cover = convertImageUrl(song.image).toImageHolder(),
        artists = song.primaryArtists.split(", ").map { name ->
            Artist(
                id = "",
                name = name,
                cover = null,
                bio = null,
                background = null,
                banners = emptyList(),
                subtitle = null,
                extras = emptyMap()
            )
        },
        album = song.albumId?.let { albumId ->
            Album(
                id = albumId,
                title = song.album,
                type = null,
                cover = convertImageUrl(song.image).toImageHolder(),
                artists = emptyList(),
                trackCount = null,
                duration = null,
                releaseDate = null,
                description = null,
                background = null,
                label = null,
                isExplicit = song.explicitContent,
                subtitle = null,
                extras = emptyMap()
            )
        },
        duration = parseDuration(song.duration),
        playedDuration = null,
        plays = song.playCount.toLongOrNull(),
        releaseDate = null,
        description = null,
        background = convertImageUrl(song.image).toImageHolder(),
        genres = if (song.language.isNotBlank()) listOf(song.language) else emptyList(),
        isrc = null,
        albumOrderNumber = null,
        albumDiscNumber = null,
        playlistAddedDate = null,
        isExplicit = song.explicitContent,
        subtitle = song.subtitle,
        extras = mapOf(
            "language" to song.language,
            "year" to song.year,
            "permaUrl" to song.permaUrl,
            "songId" to song.numericId
        ),
        isPlayable = Track.Playable.Yes,
        streamables = emptyList()
    )
}

fun songDetailToTrack(song: SongDetail): Track {
    val streamables = mutableListOf<Streamable>()
    
    if (song.streamUrls != null) {
        val urlsString = "low=${song.streamUrls.low}, medium=${song.streamUrls.medium}, high=${song.streamUrls.high}, veryHigh=${song.streamUrls.veryHigh}"
        
        streamables.add(
            Streamable.server(
                id = "server_${song.id}",
                quality = 320,
                title = "Audio Stream",
                extras = mapOf(
                    "streamUrls" to urlsString,
                    "trackId" to song.id
                )
            )
        )
    }
    
    // Build artist list with IDs
    val artistNames = song.primaryArtists.split(", ").filter { it.isNotBlank() }
    val artistIds = song.primaryArtistsId.split(", ").filter { it.isNotBlank() }
    
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
    
    return Track(
        id = song.id,
        title = song.title,
        type = Track.Type.Song,
        cover = convertImageUrl(song.image).toImageHolder(),
        artists = artists,
        album = song.albumId?.let { albumId ->
            Album(
                id = albumId,
                title = song.album,
                type = null,
                cover = convertImageUrl(song.image).toImageHolder(),
                artists = emptyList(),
                trackCount = null,
                duration = null,
                releaseDate = null,
                description = null,
                background = null,
                label = song.label,
                isExplicit = song.explicitContent,
                subtitle = null,
                extras = emptyMap()
            )
        },
        duration = parseDuration(song.duration),
        playedDuration = null,
        plays = song.playCount.toLongOrNull(),
        releaseDate = parseDate(song.releaseDate),
        description = null,
        background = convertImageUrl(song.image).toImageHolder(),
        genres = if (song.language.isNotBlank()) listOf(song.language) else emptyList(),
        isrc = null,
        albumOrderNumber = null,
        albumDiscNumber = null,
        playlistAddedDate = null,
        isExplicit = song.explicitContent,
        subtitle = song.subtitle,
        extras = mapOf(
            "language" to song.language,
            "year" to song.year,
            "permaUrl" to song.permaUrl,
            "hasLyrics" to song.hasLyrics.toString(),
            "label" to song.label,
            "copyright" to song.copyright,
            "songId" to song.numericId
        ),
        isPlayable = Track.Playable.Yes,
        streamables = streamables
    )
}

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
