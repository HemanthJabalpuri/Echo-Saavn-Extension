package dev.brahmkshatriya.echo.extension.utils

import dev.brahmkshatriya.echo.common.models.Date

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val DES_KEY = "38346591"

fun decryptUrl(encryptedUrl: String): Map<String, String>? {
    return try {
        val keySpec = SecretKeySpec(DES_KEY.toByteArray(StandardCharsets.UTF_8), "DES")
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        
        val encryptedBytes = Base64.getDecoder().decode(encryptedUrl.trim())
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        val decryptedUrl = String(decryptedBytes, StandardCharsets.UTF_8)
        
        mapOf(
            "low" to decryptedUrl.replace("_96.mp4", "_48.mp4"),
            "medium" to decryptedUrl,
            "high" to decryptedUrl.replace("_96.mp4", "_160.mp4"),
            "veryHigh" to decryptedUrl.replace("_96.mp4", "_320.mp4")
        )
    } catch (e: Exception) {
        println("DEBUG: Failed to decrypt URL: ${e.message}")
        null
    }
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
