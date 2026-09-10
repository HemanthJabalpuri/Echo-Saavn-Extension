package dev.brahmkshatriya.echo.extension.utils

import dev.brahmkshatriya.echo.common.models.Date

fun parseStreamUrls(urlsString: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    try {
        urlsString.split(",").forEach { part ->
            val (key, value) = part.trim().split("=", limit = 2)
            map[key.trim()] = value.trim()
        }
    } catch (e: Exception) {
        println("DEBUG: Failed to parse stream URLs: ${e.message}")
    }
    return map
}

fun convertImageUrl(url: String?): String {
    return url?.replace("150x150", "500x500") ?: ""
}

fun parseDuration(duration: String): Long? {
    return duration.toLongOrNull()?.times(1000)
}

fun parseDate(dateStr: String?): Date? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val parsedDate = format.parse(dateStr) ?: return null
        Date(epochTimeMs = parsedDate.time)
    } catch (e: Exception) {
        null
    }
}
