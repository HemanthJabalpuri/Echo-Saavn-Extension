package dev.brahmkshatriya.echo.extension.utils

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.serialization.json.*

object LocalLikedStore {
    private const val KEY = "liked_tracks"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ===== PUBLIC API =====

    fun getAll(settings: Settings): List<Track> {
        val set = settings.getStringSet(KEY) ?: return emptyList()
        return set.mapNotNull { parseTrackJson(it) }
    }

    fun isLiked(settings: Settings, trackId: String): Boolean {
        val set = settings.getStringSet(KEY) ?: return false
        return set.any { parseTrackId(it) == trackId }
    }

    fun like(settings: Settings, track: Track) {
        val set = settings.getStringSet(KEY)?.toMutableSet() ?: mutableSetOf()
        set.removeIf { parseTrackId(it) == track.id }
        set.add(serializeTrack(track))
        settings.putStringSet(KEY, set)
    }

    fun unlike(settings: Settings, trackId: String) {
        val set = settings.getStringSet(KEY)?.toMutableSet() ?: return
        set.removeIf { parseTrackId(it) == trackId }
        settings.putStringSet(KEY, set)
    }

    // ===== SERIALIZATION =====

    private fun serializeTrack(track: Track): String {
        return buildJsonObject {
            put("id", track.id)
            put("title", track.title)
            track.subtitle?.let { put("subtitle", it) }
            put("permaUrl", track.extras["permaUrl"] ?: "")
            (track.cover as? ImageHolder.NetworkRequestImageHolder)?.request?.url?.let {
                put("coverUrl", it)
            }
        }.toString()
    }

    private fun parseTrackJson(jsonString: String): Track? {
        return try {
            val obj = json.parseToJsonElement(jsonString).jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return null
            val coverUrl = obj["coverUrl"]?.jsonPrimitive?.content

            Track(
                id = id,
                title = obj["title"]?.jsonPrimitive?.content ?: "",
                subtitle = obj["subtitle"]?.jsonPrimitive?.content,
                cover = coverUrl?.toImageHolder(),
                extras = mapOf(
                    "permaUrl" to (obj["permaUrl"]?.jsonPrimitive?.content ?: "")
                ),
                streamables = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrackId(jsonString: String): String? {
        return try {
            json.parseToJsonElement(jsonString).jsonObject["id"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}
