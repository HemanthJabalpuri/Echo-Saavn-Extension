package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class PlaylistApi : BaseApi() {

    suspend fun search(
        query: String,
        page: Int = 1,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): JsonObject = executeRequest(
        buildUrl("search.getPlaylistResults", mapOf(
            "q" to query,
            "p" to page.toString(),
            "n" to limit.toString()
        ))
    )

    suspend fun getDetails(playlistId: String): JsonObject = executeRequest(
        buildUrl("webapi.get", mapOf(
            "type" to "playlist",
            "token" to playlistId,
            "p" to "1",
            "n" to "100"
        ))
    )
}
