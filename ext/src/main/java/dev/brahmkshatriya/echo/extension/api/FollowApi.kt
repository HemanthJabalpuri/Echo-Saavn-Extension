package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class FollowApi : AuthenticatedBaseApi() {

    suspend fun follow(entityId: String, entityType: String = "artist"): JsonObject =
        executeAuthed(
            call = "social.follow",
            params = mapOf(
                "entity_id" to entityId,
                "type" to entityType,
            ),
        )

    suspend fun unfollow(entityId: String, entityType: String = "artist"): JsonObject =
        executeAuthed(
            call = "social.unfollow",
            params = mapOf(
                "entity_id" to entityId,
                "type" to entityType,
            ),
        )
}
