package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

import dev.brahmkshatriya.echo.extension.utils.Logger

class LibraryApi : AuthenticatedBaseApi() {

    suspend fun getAll(): JsonObject {
        Logger.d("LibraryApi", "Fetching library")
        return executeAuthed(call = "library.getAll")
    }

    suspend fun getDetails(
        entityIds: List<String>,
        entityType: String = "song",
        count: Int = 50,
    ): JsonObject {
        Logger.d("LibraryApi", "Fetching details for ${entityIds.size} $entityType(s)")
        return executeAuthed(
            call = "library.getDetails",
            params = mapOf(
                "entity_ids" to entityIds.joinToString(","),
                "entity_type" to entityType,
                "n" to count.toString(),
            ),
        )
    }

}
