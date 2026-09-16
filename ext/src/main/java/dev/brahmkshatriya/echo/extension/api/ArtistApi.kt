package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class ArtistApi : BaseApi() {

    suspend fun search(
        query: String,
        page: Int = 1,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): JsonObject = executeRequest(
        call = "search.getArtistResults",
        params = mapOf(
            "q" to query,
            "p" to page.toString(),
            "n" to limit.toString()
        )
    )

    suspend fun getDetails(
        token: String?,
        artistId: String?,
        songCount: Int = DEFAULT_SONG_COUNT,
        albumCount: Int = DEFAULT_ALBUM_COUNT,
        page: Int = 1,
        subType: String = "",
        more: Boolean = false
    ): JsonObject = when {
        !token.isNullOrBlank() -> getDetailsByToken(token, songCount, albumCount, page, subType, more)
        !artistId.isNullOrBlank() -> getDetailsById(artistId, songCount, albumCount, page, subType, more)
        else -> throw Exception("Either token or artistId must be provided")
    }

    private suspend fun getDetailsById(
        artistId: String,
        songCount: Int,
        albumCount: Int,
        page: Int,
        subType: String,
        more: Boolean
    ): JsonObject = executeRequest(
        call = "artist.getArtistPageDetails",
        params = buildMap {
            put("artistId", artistId)
            put("n_song", songCount.toString())
            put("n_album", albumCount.toString())
            put("page", (page - 1).toString())
            put("sub_type", subType)
            if (more) put("more", "true")
            put("category", "popularity")
            put("sort_order", "asc")
        }
    )

    private suspend fun getDetailsByToken(
        token: String,
        songCount: Int,
        albumCount: Int,
        page: Int,
        subType: String,
        more: Boolean
    ): JsonObject = executeRequest(
        call = "webapi.get",
        params = buildMap {
            put("type", "artist")
            put("token", token)
            put("n_song", songCount.toString())
            put("n_album", albumCount.toString())
            put("p", (page - 1).toString())
            put("sub_type", subType)
            if (more) put("more", "true")
            put("category", "popularity")
            put("sort_order", "asc")
        }
    )
}
