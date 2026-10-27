package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.LikeClient
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.helpers.ClientException

import kotlinx.serialization.json.*

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.utils.Logger

class LikeClientImpl(
    private val api: JioSaavnApi,
) : LikeClient {

    override suspend fun isItemLiked(item: EchoMediaItem): Boolean {
        val type = when (item) {
            is Track -> "song"
            is Album -> "album"
            else -> return false
        }
        return try {
            val library = api.library.getAll()
            val ids = library[type]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList()
            item.id in ids
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("LikeClient", "Failed to check like status for $type/${item.id}", e)
            false
        }
    }

    override suspend fun likeItem(item: EchoMediaItem, shouldLike: Boolean) {
        val (id, type) = when (item) {
            is Track -> item.id to "song"
            is Album -> item.id to "album"
            else -> return
        }
        try {
            val response = if (shouldLike) api.library.add(id, type)
                           else api.library.delete(id, type)
            val status = response["status"]?.jsonPrimitive?.contentOrNull
            if (status != "success") {
                Logger.e("LikeClient", "library.${if (shouldLike) "add" else "delete"} failed: status=$status")
            }
        } catch (e: ClientException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("LikeClient", "Failed to ${if (shouldLike) "like" else "unlike"} $type/$id", e)
        }
    }

}
