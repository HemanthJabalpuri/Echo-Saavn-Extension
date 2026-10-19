package dev.brahmkshatriya.echo.extension.api

import dev.brahmkshatriya.echo.common.helpers.ClientException

import kotlinx.serialization.json.JsonObject

import dev.brahmkshatriya.echo.extension.SaavnDependencies
import dev.brahmkshatriya.echo.extension.utils.Logger

class LibraryApi : BaseApi() {

    suspend fun getAll(): JsonObject {
        val cookie = SaavnDependencies.sessionCookie
            ?: throw ClientException.LoginRequired()
        Logger.d("LibraryApi", "Fetching library")
        return executeRequest(
            call = "library.getAll",
            headers = mapOf("Cookie" to cookie)
        )
    }

    suspend fun getDetails(
        entityIds: List<String>,
        entityType: String = "song",
        count: Int = 50
    ): JsonObject {
        val cookie = SaavnDependencies.sessionCookie
            ?: throw ClientException.LoginRequired()
        Logger.d("LibraryApi", "Fetching details for ${entityIds.size} $entityType(s)")
        return executeRequest(
            call = "library.getDetails",
            params = mapOf(
                "entity_ids" to entityIds.joinToString(","),
                "entity_type" to entityType,
                "n" to count.toString()
            ),
            headers = mapOf("Cookie" to cookie)
        )
    }

}
