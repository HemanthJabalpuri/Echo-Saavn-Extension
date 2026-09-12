package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.extension.JioSaavnParser.*
import dev.brahmkshatriya.echo.extension.utils.*

import dev.brahmkshatriya.echo.extension.parser.*

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
