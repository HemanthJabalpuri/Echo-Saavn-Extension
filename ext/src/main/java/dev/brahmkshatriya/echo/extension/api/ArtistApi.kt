package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class ArtistApi : BaseApi() {

    suspend fun search(
        query: String,
        page: Int = 1,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): JsonObject = executeRequest(
        buildUrl("search.getArtistResults", mapOf(
            "q" to query,
            "p" to page.toString(),
            "n" to limit.toString()
        ))
    )

    suspend fun getDetails(
        token: String?,
        artistId: String?,
        songCount: Int = DEFAULT_SONG_COUNT,
        albumCount: Int = DEFAULT_ALBUM_COUNT,
        page: Int = 1
    ): JsonObject = when {
        !artistId.isNullOrBlank() -> getDetailsById(artistId, songCount, albumCount, page)
        !token.isNullOrBlank() -> getDetailsByToken(token, songCount, albumCount, page)
        else -> throw Exception("Either token or artistId must be provided")
    }

    private suspend fun getDetailsById(
        artistId: String,
        songCount: Int,
        albumCount: Int,
        page: Int
    ): JsonObject = executeRequest(
        buildUrl("artist.getArtistPageDetails", mapOf(
            "artistId" to artistId,
            "n_song" to songCount.toString(),
            "n_album" to albumCount.toString(),
            "page" to (page - 1).toString(),
            "sub_type" to "",
            "category" to "popularity",
            "sort_order" to "asc"
        ))
    )

    private suspend fun getDetailsByToken(
        token: String,
        songCount: Int,
        albumCount: Int,
        page: Int
    ): JsonObject = executeRequest(
        buildUrl("webapi.get", mapOf(
            "type" to "artist",
            "token" to token,
            "n_song" to songCount.toString(),
            "n_album" to albumCount.toString(),
            "p" to (page - 1).toString(),
            "sub_type" to "",
            "category" to "popularity",
            "sort_order" to "asc"
        ))
    )
}
