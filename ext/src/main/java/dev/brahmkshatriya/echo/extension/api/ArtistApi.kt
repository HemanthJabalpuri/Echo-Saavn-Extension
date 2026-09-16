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
        token: String,
        songCount: Int = 50,
        albumCount: Int = 50,
        page: Int = 1,
        subType: String = ""
    ): JsonObject = executeRequest(
        call = "webapi.get",
        params = buildMap {
            put("type", "artist")
            put("token", token)
            put("n_song", songCount.toString())
            put("n_album", albumCount.toString())
            put("p", (page - 1).toString())
            put("sub_type", subType)
            if (subType.isNotBlank()) {
                put("more", "true")
            }
            put("category", "popularity")
            put("sort_order", "asc")
        }
    )

}
