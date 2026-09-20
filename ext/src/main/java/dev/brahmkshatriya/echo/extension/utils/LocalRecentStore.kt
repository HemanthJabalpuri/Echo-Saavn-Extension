package dev.brahmkshatriya.echo.extension.utils

import dev.brahmkshatriya.echo.common.models.ImageHolder
import dev.brahmkshatriya.echo.common.models.ImageHolder.Companion.toImageHolder
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.settings.Settings
import kotlinx.serialization.json.*

object LocalRecentStore {
    private const val KEY = "recent_tracks"
    private const val MAX_SIZE = 20
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getAll(settings: Settings): List<Track> {
        val set = settings.getStringSet(KEY) ?: return emptyList()
        return set
            .mapNotNull { parseEntry(it) }
            .sortedByDescending { it.playedAt }
            .map { it.track }
    }

    fun record(settings: Settings, track: Track) {
        val set = settings.getStringSet(KEY)?.toMutableSet() ?: mutableSetOf()
        // Remove existing entry with same track ID
        set.removeIf { parseTrackId(it) == track.id }
        // Add new entry
        set.add(serializeEntry(track, System.currentTimeMillis()))
        // Trim to MAX_SIZE
        val trimmed = set
            .mapNotNull { parseEntry(it) }
            .sortedByDescending { it.playedAt }
            .take(MAX_SIZE)
            .map { serializeEntry(it.track, it.playedAt) }
            .toSet()
        settings.putStringSet(KEY, trimmed)
    }

    // ===== SERIALIZATION =====

    private data class Entry(val track: Track, val playedAt: Long)

    private fun serializeEntry(track: Track, playedAt: Long): String {
        return buildJsonObject {
            put("id", track.id)
            put("title", track.title)
            track.subtitle?.let { put("subtitle", it) }
            put("permaUrl", track.extras["permaUrl"] ?: "")
            (track.cover as? ImageHolder.NetworkRequestImageHolder)?.request?.url?.let {
                put("coverUrl", it)
            }
            put("playedAt", playedAt)
        }.toString()
    }

    private fun parseEntry(jsonString: String): Entry? {
        return try {
            val obj = json.parseToJsonElement(jsonString).jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return null
            val playedAt = obj["playedAt"]?.jsonPrimitive?.longOrNull ?: return null
            val coverUrl = obj["coverUrl"]?.jsonPrimitive?.content

            val track = Track(
                id = id,
                title = obj["title"]?.jsonPrimitive?.content ?: "",
                subtitle = obj["subtitle"]?.jsonPrimitive?.content,
                cover = coverUrl?.toImageHolder(),
                extras = mapOf(
                    "permaUrl" to (obj["permaUrl"]?.jsonPrimitive?.content ?: "")
                ),
                streamables = emptyList()
            )
            Entry(track, playedAt)
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
