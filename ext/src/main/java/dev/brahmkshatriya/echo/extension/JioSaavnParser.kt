package dev.brahmkshatriya.echo.extension

import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class JioSaavnParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    companion object {
        private const val DES_KEY = "38346591"
    }
    
    fun decryptUrl(encryptedUrl: String): StreamUrls? {
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

    fun parseSearchAll(jsonString: String): SearchAllResult {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        
        return SearchAllResult(
            songs = parseSongResults(jsonObject["results"]?.jsonArray),
            albums = parseAlbumResults(jsonObject["albums"]?.jsonObject?.get("data")?.jsonArray),
            artists = parseArtistResults(jsonObject["artists"]?.jsonObject?.get("data")?.jsonArray),
            playlists = parsePlaylistResults(jsonObject["playlists"]?.jsonObject?.get("data")?.jsonArray)
        )
    }
    
    fun parseSongSearchResults(jsonString: String): List<SongResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseSongResults(results)
    }
    
    fun parseSimilarSongs(jsonString: String): List<SongResult> {
        return try {
            println("DEBUG: Similar songs raw response (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            println("DEBUG: JSON element type: ${jsonElement::class.simpleName}")
            
            val jsonArray = jsonElement.jsonArray
            println("DEBUG: Similar songs array size: ${jsonArray.size}")
            
            val results = parseSongResults(jsonArray)
            println("DEBUG: Parsed ${results.size} similar songs")
            results
        } catch (e: Exception) {
            println("DEBUG: Failed to parse similar songs: ${e.message}")
            println("DEBUG: Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    
    fun parseStationId(jsonString: String): String? {
        return try {
            println("DEBUG: Parsing station ID from response (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            val stationId = jsonElement.jsonObject["stationid"]?.jsonPrimitive?.content
            println("DEBUG: Extracted station ID: $stationId")
            stationId
        } catch (e: Exception) {
            println("DEBUG: Failed to parse station ID: ${e.message}")
            null
        }
    }
    
    fun parseSongSuggestions(jsonString: String): List<SongResult> {
        return try {
            println("DEBUG: Parsing song suggestions (first 500 chars): ${jsonString.take(500)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject
            
            val songs = mutableListOf<SongResult>()
            
            jsonObject.forEach { (key, value) ->
                if (key != "stationid") {
                    try {
                        val songObject = value.jsonObject["song"]?.jsonObject
                        if (songObject != null) {
                            val song = parseSongFromJson(songObject)
                            song?.let { songs.add(it) }
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Failed to parse song at key $key: ${e.message}")
                    }
                }
            }
            
            println("DEBUG: Successfully parsed ${songs.size} song suggestions from station")
            songs
        } catch (e: Exception) {
            println("DEBUG: Failed to parse song suggestions: ${e.message}")
            println("DEBUG: Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    
    fun parseAlbumSearchResults(jsonString: String): List<AlbumResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseAlbumResults(results)
    }
    
    fun parseArtistSearchResults(jsonString: String): List<ArtistResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parseArtistResults(results)
    }
    
    fun parsePlaylistSearchResults(jsonString: String): List<PlaylistResult> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val results = jsonObject["results"]?.jsonArray ?: return emptyList()
        return parsePlaylistResults(results)
    }

    fun parseSongDetails(jsonString: String): List<SongDetail> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val songsArray = jsonObject["songs"]?.jsonArray ?: return emptyList()
            songsArray.mapNotNull { element ->
                parseSongDetail(element.jsonObject)
            }
        } catch (e: Exception) {
            println("DEBUG: Failed to parse song details: ${e.message}")
            emptyList()
        }
    }

    fun parseAlbumDetails(jsonString: String): AlbumDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseAlbumDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse album details: ${e.message}")
            null
        }
    }

    fun parseArtistDetails(jsonString: String): ArtistDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parseArtistDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse artist details: ${e.message}")
            null
        }
    }

    fun parsePlaylistDetails(jsonString: String): PlaylistDetail? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            parsePlaylistDetail(jsonObject)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse playlist details: ${e.message}")
            null
        }
    }

    fun parseHomeData(jsonString: String): HomeData? {
        return try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            
            val nowTrending = mutableListOf<MediaItem>()
            val topPlaylists = mutableListOf<PlaylistResult>()
            val newAlbums = mutableListOf<MediaItem>()
            val topCharts = mutableListOf<PlaylistResult>()
            

            jsonObject["new_trending"]?.jsonArray?.forEach { item ->
                try {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    
                    when (type) {
                        "song" -> parseSongFromJson(obj)?.let { nowTrending.add(MediaItem.Song(it)) }
                        "album" -> parseAlbumFromJson(obj)?.let { nowTrending.add(MediaItem.Album(it)) }
                        "playlist" -> parsePlaylistFromJson(obj)?.let { nowTrending.add(MediaItem.Playlist(it)) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse trending item: ${e.message}")
                }
            }
            
            jsonObject["top_playlists"]?.jsonArray?.forEach { item ->
                try {
                    parsePlaylistFromJson(item.jsonObject)?.let { topPlaylists.add(it) }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse top playlist: ${e.message}")
                }
            }
            
            jsonObject["new_albums"]?.jsonArray?.forEach { item ->
                try {
                    val obj = item.jsonObject
                    val type = obj["type"]?.jsonPrimitive?.content ?: ""
                    
                    when (type) {
                        "album" -> parseAlbumFromJson(obj)?.let { newAlbums.add(MediaItem.Album(it)) }
                        "song" -> parseSongFromJson(obj)?.let { newAlbums.add(MediaItem.Song(it)) }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse new album: ${e.message}")
                }
            }
            
            jsonObject["charts"]?.jsonArray?.forEach { item ->
                try {
                    parsePlaylistFromJson(item.jsonObject)?.let { topCharts.add(it) }
                } catch (e: Exception) {
                    println("DEBUG: Failed to parse chart: ${e.message}")
                }
            }
            
            HomeData(
                nowTrending = nowTrending,
                topPlaylists = topPlaylists,
                newAlbums = newAlbums,
                topCharts = topCharts
            )
        } catch (e: Exception) {
            println("ERROR parsing home data: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private fun parseSongFromJson(obj: JsonObject): SongResult? {
        return try {
            SongResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content 
                    ?: obj["song"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                album = decodeHtml(obj["album"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("album")?.jsonPrimitive?.content ?: ""),
                albumId = obj["albumid"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("albumid")?.jsonPrimitive?.content,
                duration = obj["duration"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("duration")?.jsonPrimitive?.content ?: "0",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                primaryArtists = decodeHtml(obj["primary_artists"]?.jsonPrimitive?.content 
                    ?: obj["music"]?.jsonPrimitive?.content ?: ""),
                year = obj["year"]?.jsonPrimitive?.content ?: "",
                playCount = obj["play_count"]?.jsonPrimitive?.content ?: "0",
                type = obj["type"]?.jsonPrimitive?.content ?: "song",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAlbumFromJson(obj: JsonObject): AlbumResult? {
        return try {
            AlbumResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content 
                    ?: obj["header_desc"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                year = obj["year"]?.jsonPrimitive?.content ?: "",
                songCount = obj["song_count"]?.jsonPrimitive?.content 
                    ?: obj["more_info"]?.jsonObject?.get("song_count")?.jsonPrimitive?.content ?: "0",
                type = obj["type"]?.jsonPrimitive?.content ?: "",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePlaylistFromJson(obj: JsonObject): PlaylistResult? {
        return try {
            PlaylistResult(
                id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null,
                title = decodeHtml(obj["title"]?.jsonPrimitive?.content 
                    ?: obj["listname"]?.jsonPrimitive?.content ?: ""),
                subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content 
                    ?: obj["header_desc"]?.jsonPrimitive?.content ?: ""),
                image = obj["image"]?.jsonPrimitive?.content ?: "",
                permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                songCount = obj["song_count"]?.jsonPrimitive?.content 
                    ?: obj["list_count"]?.jsonPrimitive?.content ?: "0",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                type = obj["type"]?.jsonPrimitive?.content ?: "",
                explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseSongResults(array: JsonArray?): List<SongResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                SongResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "song",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    year = obj["year"]?.jsonPrimitive?.content ?: "",
                    playCount = obj["play_count"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    primaryArtists = obj["more_info"]?.jsonObject?.get("artistMap")?.jsonObject
                        ?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: "",
                    albumId = obj["more_info"]?.jsonObject?.get("album_id")?.jsonPrimitive?.content,
                    album = obj["more_info"]?.jsonObject?.get("album")?.jsonPrimitive?.content ?: "",
                    duration = obj["more_info"]?.jsonObject?.get("duration")?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseAlbumResults(array: JsonArray?): List<AlbumResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val moreInfo = obj["more_info"]?.jsonObject
                val artistMap = moreInfo?.get("artistMap")?.jsonObject
                
                val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
                val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""
                
                AlbumResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "album",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    year = obj["year"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content ?: "0",
                    primaryArtists = primaryArtists,
                    primaryArtistsId = primaryArtistsId
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseArtistResults(array: JsonArray?): List<ArtistResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                ArtistResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    name = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "artist",
                    role = obj["description"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun parsePlaylistResults(array: JsonArray?): List<PlaylistResult> {
        if (array == null) return emptyList()
        return array.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                PlaylistResult(
                    id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return@mapNotNull null,
                    title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
                    subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
                    image = obj["image"]?.jsonPrimitive?.content ?: "",
                    permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "playlist",
                    language = obj["language"]?.jsonPrimitive?.content ?: "",
                    explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
                    songCount = obj["more_info"]?.jsonObject?.get("song_count")?.jsonPrimitive?.content ?: "0"
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseSongDetail(obj: JsonObject): SongDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
        val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""

        val featuredArtists = artistMap?.get("featured_artists")?.jsonArray
            ?.let { extractArtistNames(it) }
            ?.ifEmpty { null }

        val encryptedMediaUrl = moreInfo?.get("encrypted_media_url")?.jsonPrimitive?.content
        val streamUrls = encryptedMediaUrl?.let { decryptUrl(it) }

        return SongDetail(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = "song",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
            playCount = obj["play_count"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            primaryArtists = primaryArtists,
            primaryArtistsId = primaryArtistsId,
            featuredArtists = featuredArtists,
            albumId = moreInfo?.get("album_url")?.jsonPrimitive?.content?.substringAfterLast("/"),
            album = moreInfo?.get("album")?.jsonPrimitive?.content ?: "",
            albumUrl = moreInfo?.get("album_url")?.jsonPrimitive?.content,
            duration = moreInfo?.get("duration")?.jsonPrimitive?.content ?: "0",
            label = moreInfo?.get("label")?.jsonPrimitive?.content ?: "",
            copyright = moreInfo?.get("copyright_text")?.jsonPrimitive?.content ?: "",
            releaseDate = moreInfo?.get("release_date")?.jsonPrimitive?.content,
            hasLyrics = moreInfo?.get("has_lyrics")?.jsonPrimitive?.content == "true",
            lyricsId = null,
            encryptedMediaUrl = encryptedMediaUrl,
            streamUrls = streamUrls,
            is320kbps = moreInfo?.get("320kbps")?.jsonPrimitive?.content == "true"
        )
    }

    private fun parseAlbumDetail(obj: JsonObject): AlbumDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject
        val artistMap = moreInfo?.get("artistMap")?.jsonObject

        val primaryArtists = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistNames(it) } ?: ""
        val primaryArtistsId = artistMap?.get("primary_artists")?.jsonArray?.let { extractArtistIds(it) } ?: ""

        val songs = obj["list"]?.jsonArray?.mapNotNull { parseSongDetail(it.jsonObject) } ?: emptyList()
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content ?: "0"

        return AlbumDetail(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = obj["type"]?.jsonPrimitive?.content ?: "album",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            year = obj["year"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            primaryArtists = primaryArtists,
            primaryArtistsId = primaryArtistsId,
            songCount = songCount,
            releaseDate = obj["year"]?.jsonPrimitive?.content, // year as release date
            songs = songs
        )
    }

    private fun parseArtistDetail(obj: JsonObject): ArtistDetail? {
        val id = obj["urls"]?.jsonObject?.get("overview")?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null

        val topSongs = obj["topSongs"]?.jsonArray?.mapNotNull { parseSongDetail(it.jsonObject) } ?: emptyList()
        val topAlbums = obj["topAlbums"]?.jsonArray?.mapNotNull { parseAlbumResults(buildJsonArray { add(it) })?.firstOrNull() } ?: emptyList()

        return ArtistDetail(
            id = id,
            name = decodeHtml(obj["name"]?.jsonPrimitive?.content ?: ""),
            subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "",
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            followerCount = obj["follower_count"]?.jsonPrimitive?.content ?: "0",
            type = obj["type"]?.jsonPrimitive?.content ?: "artist",
            isVerified = obj["isVerified"]?.jsonPrimitive?.booleanOrNull ?: false,
            dominantLanguage = obj["dominantLanguage"]?.jsonPrimitive?.content ?: "",
            dominantType = obj["dominantType"]?.jsonPrimitive?.content ?: "",
            topSongs = topSongs,
            topAlbums = topAlbums
        )
    }

    private fun parsePlaylistDetail(obj: JsonObject): PlaylistDetail? {
        val id = obj["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/") ?: return null
        val moreInfo = obj["more_info"]?.jsonObject

        val songs = obj["list"]?.jsonArray?.mapNotNull { parseSongDetail(it.jsonObject) } ?: emptyList()
        val songCount = moreInfo?.get("song_count")?.jsonPrimitive?.content
            ?: obj["list_count"]?.jsonPrimitive?.content
            ?: "0"
        val followerCount = moreInfo?.get("follower_count")?.jsonPrimitive?.content
            ?: obj["follower_count"]?.jsonPrimitive?.content
            ?: "0"

        return PlaylistDetail(
            id = id,
            title = decodeHtml(obj["title"]?.jsonPrimitive?.content ?: obj["listname"]?.jsonPrimitive?.content ?: ""),
            subtitle = decodeHtml(obj["subtitle"]?.jsonPrimitive?.content ?: ""),
            image = obj["image"]?.jsonPrimitive?.content ?: "",
            permaUrl = obj["perma_url"]?.jsonPrimitive?.content ?: "",
            type = obj["type"]?.jsonPrimitive?.content ?: "playlist",
            language = obj["language"]?.jsonPrimitive?.content ?: "",
            explicitContent = obj["explicit_content"]?.jsonPrimitive?.content == "1",
            songCount = songCount,
            followerCount = followerCount,
            songs = songs
        )
    }
    
    private fun extractArtistNames(array: JsonArray): String {
        return array.mapNotNull { 
            it.jsonObject["name"]?.jsonPrimitive?.content 
        }.joinToString(", ")
    }

    private fun extractArtistIds(array: JsonArray): String {
        return array.mapNotNull { 
            it.jsonObject["perma_url"]?.jsonPrimitive?.content?.substringAfterLast("/")
        }.joinToString(", ")
    }

    private fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}

data class StreamUrls(
    val low: String,      
    val medium: String,   
    val high: String,     
    val veryHigh: String  
)

data class SearchAllResult(
    val songs: List<SongResult>,
    val albums: List<AlbumResult>,
    val artists: List<ArtistResult>,
    val playlists: List<PlaylistResult>
)

data class SongResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val playCount: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val albumId: String?,
    val album: String,
    val duration: String
)

data class AlbumResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val explicitContent: Boolean,
    val songCount: String,
    val primaryArtists: String = "",
    val primaryArtistsId: String = ""
)

data class ArtistResult(
    val id: String,
    val name: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val role: String
)

data class PlaylistResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val explicitContent: Boolean,
    val songCount: String
)

data class SongDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val playCount: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val primaryArtistsId: String,
    val featuredArtists: String?,
    val albumId: String?,
    val album: String,
    val albumUrl: String?,
    val duration: String,
    val label: String,
    val copyright: String,
    val releaseDate: String?,
    val hasLyrics: Boolean,
    val lyricsId: String?,
    val encryptedMediaUrl: String?,
    val streamUrls: StreamUrls?,
    val is320kbps: Boolean
)

data class AlbumDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val year: String,
    val explicitContent: Boolean,
    val primaryArtists: String,
    val primaryArtistsId: String,
    val songCount: String,
    val releaseDate: String?,
    val songs: List<SongDetail>
)

data class ArtistDetail(
    val id: String,
    val name: String,
    val subtitle: String,
    val image: String,
    val followerCount: String,
    val type: String,
    val isVerified: Boolean,
    val dominantLanguage: String,
    val dominantType: String,
    val topSongs: List<SongDetail>,
    val topAlbums: List<AlbumResult>
)

data class PlaylistDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val permaUrl: String,
    val type: String,
    val language: String,
    val explicitContent: Boolean,
    val songCount: String,
    val followerCount: String,
    val songs: List<SongDetail>
)
data class HomeData(
    val nowTrending: List<MediaItem>,
    val topPlaylists: List<PlaylistResult>,
    val newAlbums: List<MediaItem>,
    val topCharts: List<PlaylistResult>
)

sealed class MediaItem {
    data class Song(val data: SongResult) : MediaItem()
    data class Album(val data: AlbumResult) : MediaItem()
    data class Playlist(val data: PlaylistResult) : MediaItem()
}

