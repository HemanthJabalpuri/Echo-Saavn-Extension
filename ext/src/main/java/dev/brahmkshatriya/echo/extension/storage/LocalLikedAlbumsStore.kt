package dev.brahmkshatriya.echo.extension.storage

import dev.brahmkshatriya.echo.common.models.Album
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder

object LocalLikedAlbumsStore : LocalStore<Album>() {
    override val key = "liked_albums"

    override fun serializeItem(item: Album, json: JsonObjectBuilder) {
        json.putCommonFields(item)
    }

    override fun deserializeItem(obj: JsonObject): Album? {
        val common = obj.parseCommonFields() ?: return null
        return Album(
            id = common.id,
            title = common.title,
            subtitle = common.subtitle,
            cover = common.cover,
            extras = mapOf("permaUrl" to common.permaUrl)
        )
    }
}
