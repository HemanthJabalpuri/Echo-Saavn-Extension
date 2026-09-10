package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.extension.utils.parseStreamUrls
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest

import dev.brahmkshatriya.echo.extension.*

class TrackClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : TrackClient {

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return try {
            println("DEBUG: Loading track with ID: ${track.id}")
            val response = api.getSongDetails(track.id)
            val songDetails = parser.parseSongDetails(response).firstOrNull()
                ?: throw Exception("Track not found")
            
            songDetailToTrack(songDetails)
        } catch (e: Exception) {
            println("DEBUG: Failed to load track ${track.id}: ${e.message}")
            throw Exception("Failed to load track: ${e.message}")
        }
    }
    
    override suspend fun loadStreamableMedia(
        streamable: Streamable, 
        isDownload: Boolean
    ): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                println("DEBUG: Loading streamable media")
                
                val streamUrls = streamable.extras["streamUrls"] 
                    ?: throw Exception("No stream URLs found")
                val urls = parseStreamUrls(streamUrls)

                val sources = mutableListOf<Streamable.Source.Http>()
                
                if (urls["veryHigh"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["veryHigh"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 320,
                        title = "320kbps"
                    ))
                }
                
                if (urls["high"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["high"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 160,
                        title = "160kbps"
                    ))
                }
                
                if (urls["medium"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["medium"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 96,
                        title = "96kbps"
                    ))
                }
                
                if (urls["low"]?.isNotBlank() == true) {
                    sources.add(Streamable.Source.Http(
                        request = urls["low"]!!.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 48,
                        title = "48kbps"
                    ))
                }
                
                if (sources.isEmpty()) {
                    throw Exception("No valid stream URLs available")
                }
                
                Streamable.Media.Server(sources, false)
            }
            Streamable.MediaType.Background -> {
                throw Exception("Background streamables not supported")
            }
            Streamable.MediaType.Subtitle -> {
                throw Exception("Subtitles not supported")
            }
        }
    }

    override suspend fun loadFeed(track: Track): Feed<Shelf> {
        return try {
            println("DEBUG: Loading related tracks for: ${track.id}")

            try {
                val songId = track.extras["songId"] ?: track.id
                val stationResponse = api.createSongStation(songId)
                val stationId = parser.parseStationId(stationResponse)
                
                if (stationId != null) {
                    val suggestionsResponse = api.getSongSuggestions(stationId, limit = 20)
                    val songs = parser.parseSongSuggestions(suggestionsResponse)
                    
                    if (songs.isNotEmpty()) {
                        val tracks = songs.map { songResultToTrack(it) }
                        
                        return listOf(
                            Shelf.Lists.Tracks(
                                id = "similar_tracks",
                                title = "Similar Tracks",
                                list = tracks,
                                subtitle = "You might also like"
                            )
                        ).toFeed()
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Station-based suggestions failed in loadFeed: ${e.message}")
            }
            
            emptyList<Shelf>().toFeed()
        } catch (e: Exception) {
            println("DEBUG: Failed to load similar tracks: ${e.message}")
            emptyList<Shelf>().toFeed()
        }
    }

}
