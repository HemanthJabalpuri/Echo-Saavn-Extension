package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class AlbumApi : BaseApi() {

    suspend fun search(
        query: String,
        page: Int = 1,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): JsonObject = executeRequest(
        call = "search.getAlbumResults",
        params = mapOf(
            "q" to query,
            "p" to page.toString(),
            "n" to limit.toString()
        )
    )

    suspend fun getDetails(albumId: String): JsonObject = executeRequest(
        call = "webapi.get",
        params = mapOf(
            "type" to "album",
            "token" to albumId
        )
    )
}
