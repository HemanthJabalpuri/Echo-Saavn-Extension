package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class JioSaavnApi {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder()
            builder.addHeader("Accept", "application/json")
            builder.addHeader("Accept-Language", "en-US,en;q=0.9")
            builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            chain.proceed(builder.build())
        }
        .build()
    
    companion object {
        private const val BASE_URL = "https://www.jiosaavn.com/api.php"
        
        private const val SEARCH_ALL = "search.getResults"
        private const val SEARCH_SONGS = "search.getResults"
        private const val SEARCH_ALBUMS = "search.getAlbumResults"
        private const val SEARCH_ARTISTS = "search.getArtistResults"
        private const val SEARCH_PLAYLISTS = "search.getPlaylistResults"

        private const val SONG_DETAILS = "song.getDetails"
        private const val ALBUM_DETAILS = "content.getAlbumDetails"
        private const val ARTIST_DETAILS = "artist.getArtistPageDetails"
        private const val PLAYLIST_DETAILS = "playlist.getDetails"
    }

    suspend fun searchAll(query: String, page: Int = 1, limit: Int = 20): String {
        val url = buildUrl(
            call = SEARCH_ALL,
            params = mapOf(
                "q" to query,
                "p" to page.toString(),
                "n" to limit.toString(),
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }

    suspend fun searchSongs(query: String, page: Int = 1, limit: Int = 20): String {
        val url = buildUrl(
            call = SEARCH_SONGS,
            params = mapOf(
                "q" to query,
                "p" to page.toString(),
                "n" to limit.toString(),
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    suspend fun searchAlbums(query: String, page: Int = 1, limit: Int = 20): String {
        val url = buildUrl(
            call = SEARCH_ALBUMS,
            params = mapOf(
                "q" to query,
                "p" to page.toString(),
                "n" to limit.toString(),
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun searchArtists(query: String, page: Int = 1, limit: Int = 20): String {
        val url = buildUrl(
            call = SEARCH_ARTISTS,
            params = mapOf(
                "q" to query,
                "p" to page.toString(),
                "n" to limit.toString(),
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun searchPlaylists(query: String, page: Int = 1, limit: Int = 20): String {
        val url = buildUrl(
            call = SEARCH_PLAYLISTS,
            params = mapOf(
                "q" to query,
                "p" to page.toString(),
                "n" to limit.toString(),
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun getSongDetails(songId: String): String {
        val url = buildUrl(
            call = SONG_DETAILS,
            params = mapOf(
                "pids" to songId,
                "cc" to "in",
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun getAlbumDetails(albumId: String): String {
        val url = buildUrl(
            call = ALBUM_DETAILS,
            params = mapOf(
                "albumid" to albumId,
                "cc" to "in",
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun getArtistDetails(artistId: String, songCount: Int = 10, albumCount: Int = 10): String {
        val url = buildUrl(
            call = ARTIST_DETAILS,
            params = mapOf(
                "artistId" to artistId,
                "n_song" to songCount.toString(),
                "n_album" to albumCount.toString(),
                "p" to "1",
                "sub_type" to "",
                "category" to "popular",
                "sort_order" to "asc",
                "cc" to "in",
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    suspend fun getPlaylistDetails(playlistId: String): String {
        val url = buildUrl(
            call = PLAYLIST_DETAILS,
            params = mapOf(
                "listid" to playlistId,
                "cc" to "in",
                "_format" to "json",
                "_marker" to "0",
                "api_version" to "4",
                "ctx" to "web6dot0"
            )
        )
        return executeRequest(url)
    }
    
    private fun buildUrl(call: String, params: Map<String, String>): String {
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        return "$BASE_URL?__call=$call&$queryString"
    }
    
    private suspend fun executeRequest(url: String): String {
    
        println("DEBUG: Request URL: $url")
    
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }
        
        return response.body?.string() ?: throw Exception("Empty response body")
    }
    
    suspend fun createSongStation(songId: String): String {
        val encodedSongId = java.net.URLEncoder.encode(songId, "UTF-8")
        val entityId = "[\"$encodedSongId\"]"
        val url = "$BASE_URL?__call=webradio.createEntityStation&api_version=4&_format=json&_marker=0&ctx=android&entity_id=$entityId&entity_type=queue"
        println("DEBUG: Creating song station from: $url")
        val response = executeRequest(url)
        println("DEBUG: Station creation response: $response")
        return response
    }
    
    suspend fun getSongSuggestions(stationId: String, limit: Int = 20): String {
        val url = "$BASE_URL?__call=webradio.getSong&api_version=4&_format=json&_marker=0&ctx=android&stationid=$stationId&k=$limit"
        println("DEBUG: Fetching song suggestions from station: $stationId")
        val response = executeRequest(url)
        println("DEBUG: Song suggestions response length: ${response.length}")
        return response
    }
    @Deprecated("Use createSongStation and getSongSuggestions instead", ReplaceWith("getSongSuggestions(createSongStation(trackId), limit)"))
    suspend fun getSimilarSongs(trackId: String): String {
        val url = "$BASE_URL?__call=reco.getreco&api_version=4&_format=json&_marker=0&ctx=wap6dot0&language=english&pid=$trackId"
        println("DEBUG: Fetching similar songs from: $url (DEPRECATED - unreliable)")
        val response = executeRequest(url)
        println("DEBUG: Similar songs response length: ${response.length}")
        return response
    }
    
    suspend fun getHomeData(language: String = "hindi"): String {
        val url = "$BASE_URL?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=wap6dot0"
        
        val request = Request.Builder()
            .url(url)
            .header("Cookie", "L=$language;")
            .get()
            .build()
        
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }
        
        return response.body?.string() ?: throw Exception("Empty response body")
    }
}
