package dev.brahmkshatriya.echo.extension.api

import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.extension.SaavnDependencies
import kotlinx.serialization.json.JsonObject

open class AuthenticatedBaseApi : BaseApi() {

    protected fun requireCookie(): String =
        SaavnDependencies.sessionCookie ?: throw ClientException.LoginRequired()

    protected suspend fun executeAuthed(
        call: String,
        params: Map<String, String> = emptyMap(),
        extraHeaders: Map<String, String> = emptyMap(),
    ): JsonObject = executeRequest(
        call = call,
        params = params,
        headers = extraHeaders + mapOf("Cookie" to requireCookie()),
    )
}
