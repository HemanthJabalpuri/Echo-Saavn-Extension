package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class AuthApi : BaseApi() {

    suspend fun getConfigDetailsCSR(cookie: String): JsonObject = executeRequest(
        call = "webapi.getConfigDetailsCSR",
        headers = mapOf("Cookie" to cookie),
    )

}
