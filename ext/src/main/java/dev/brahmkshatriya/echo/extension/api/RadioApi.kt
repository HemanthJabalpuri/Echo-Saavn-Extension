package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder

class RadioApi : BaseApi() {

    override val defaultCtx: String = "android"

    suspend fun createSongStation(songId: String): JsonObject {
        val encodedSongId = URLEncoder.encode(songId, "UTF-8")
        val entityId = "[\"$encodedSongId\"]"
        return executeRequest(
            buildUrl("webradio.createEntityStation", mapOf(
                "entity_id" to entityId,
                "entity_type" to "queue"
            ))
        )
    }

    suspend fun getSongSuggestions(
        stationId: String,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): JsonObject = executeRequest(
        buildUrl("webradio.getSong", mapOf(
            "stationid" to stationId,
            "k" to limit.toString(),
            "next" to "1"
        ))
    )
}
