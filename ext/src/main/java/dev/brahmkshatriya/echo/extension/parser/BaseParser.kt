package dev.brahmkshatriya.echo.extension.parser

import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import dev.brahmkshatriya.echo.extension.parser.*

open class BaseParser {
    
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    protected fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
    
    protected fun decryptUrl(encryptedUrl: String): StreamUrls? {
        return try {
            val keySpec = SecretKeySpec(DES_KEY.toByteArray(StandardCharsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            
            val encryptedBytes = Base64.getDecoder().decode(encryptedUrl.trim())
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val decryptedUrl = String(decryptedBytes, StandardCharsets.UTF_8)
            
            StreamUrls(
                low = decryptedUrl.replace("_96.mp4", "_48.mp4"),
                medium = decryptedUrl,
                high = decryptedUrl.replace("_96.mp4", "_160.mp4"),
                veryHigh = decryptedUrl.replace("_96.mp4", "_320.mp4")
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to decrypt URL: ${e.message}")
            null
        }
    }
    
    protected fun extractArtistNames(array: JsonArray): String {
        return array.mapNotNull {
            it.jsonObject["name"]?.jsonPrimitive?.content
        }.joinToString(", ")
    }
    
    protected fun extractArtistIds(array: JsonArray): String {
        return array.mapNotNull {
            it.jsonObject["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/")
        }.joinToString(", ")
    }
    
    companion object {
        private const val DES_KEY = "38346591"
    }
}
