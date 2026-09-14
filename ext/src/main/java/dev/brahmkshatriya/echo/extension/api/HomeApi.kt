package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class HomeApi : BaseApi() {

    suspend fun getHomeData(language: String = "hindi"): JsonObject = executeRequest(
        call = "webapi.getLaunchData",
        headers = mapOf("Cookie" to "L=$language;")
    )
}
