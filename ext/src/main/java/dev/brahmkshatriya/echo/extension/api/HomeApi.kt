package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class HomeApi : BaseApi() {

    suspend fun getHomeData(language: String = "hindi"): JsonObject {
        val url = buildUrl("webapi.getLaunchData", emptyMap())
        return executeRequest(url, headers = mapOf("Cookie" to "L=$language;"))
    }

}
