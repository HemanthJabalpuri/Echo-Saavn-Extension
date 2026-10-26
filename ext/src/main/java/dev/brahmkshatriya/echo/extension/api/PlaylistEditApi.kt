package dev.brahmkshatriya.echo.extension.api

import kotlinx.serialization.json.JsonObject

class PlaylistEditApi : AuthenticatedBaseApi() {

    suspend fun listMyPlaylists(): JsonObject =
        executeAuthed(
            call = "playlist.list",
            params = mapOf(
                "all_playlists" to "true",
                "contents" to "1",
                "onlypids" to "true",
            ),
        )

    suspend fun add(listId: String, contents: String): JsonObject =
        executeAuthed(
            call = "playlist.add",
            params = mapOf(
                "listid" to listId,
                "contents" to contents,
            ),
        )

    suspend fun remove(listId: String, pid: String): JsonObject =
        executeAuthed(
            call = "playlist.remove",
            params = mapOf(
                "listid" to listId,
                "pid" to pid,
            ),
        )

}
