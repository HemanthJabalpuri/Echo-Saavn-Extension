package dev.brahmkshatriya.echo.extension.storage

import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder

object LocalRecentStore : LocalStore<Track>() {
    override val key = "recent_tracks"
    override val maxSize = 20

    override fun getSortKey(): Long = System.currentTimeMillis()

    override fun serializeItem(item: Track, json: JsonObjectBuilder) {
        json.putCommonFields(item)
    }

    override fun deserializeItem(obj: JsonObject): Track? {
        val common = obj.parseCommonFields() ?: return null
        return Track(
            id = common.id,
            title = common.title,
            subtitle = common.subtitle,
            cover = common.cover,
            extras = mapOf("permaUrl" to common.permaUrl),
            streamables = emptyList()
        )
    }
}
