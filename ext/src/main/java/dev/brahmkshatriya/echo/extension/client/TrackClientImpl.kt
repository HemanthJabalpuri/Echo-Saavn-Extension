package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed
import dev.brahmkshatriya.echo.common.models.NetworkRequest.Companion.toGetRequest

import dev.brahmkshatriya.echo.extension.JioSaavnApi
import dev.brahmkshatriya.echo.extension.JioSaavnParser
import dev.brahmkshatriya.echo.extension.utils.*

class TrackClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : TrackClient {

    override suspend fun loadTrack(track: Track, isDownload: Boolean): Track {
        return track
    }

    override suspend fun loadStreamableMedia(
        streamable: Streamable,
        isDownload: Boolean
    ): Streamable.Media {
        return when (streamable.type) {
            Streamable.MediaType.Server -> {
                val encryptedUrl = streamable.extras["encryptedMediaUrl"]
                    ?: throw Exception("No encrypted URL found")
                
                // Decrypt on demand
                val urls = decryptUrl(encryptedUrl)
                    ?: throw Exception("Failed to decrypt URL")
                
                val sources = mutableListOf<Streamable.Source.Http>()
                
                urls["veryHigh"]?.let {
                    sources.add(Streamable.Source.Http(
                        request = it.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 320,
                        title = "320kbps"
                    ))
                }
                
                urls["high"]?.let {
                    sources.add(Streamable.Source.Http(
                        request = it.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 160,
                        title = "160kbps"
                    ))
                }
                
                urls["medium"]?.let {
                    sources.add(Streamable.Source.Http(
                        request = it.toGetRequest(),
                        type = Streamable.SourceType.Progressive,
                        quality = 96,
                        title = "96kbps"
                    ))
                }
                
                urls["low"]?.let {
                    sources.add(Streamable.Source.Http(
                        request = it.toGetRequest(),
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
            val songId = track.extras["songId"] ?: track.id
            val response = api.createSongStation(songId)
            val stationId = parser.radio.parseStationId(response)
            
            if (stationId != null) {
                val suggestionsResponse = api.getSongSuggestions(stationId, limit = 20)
                val songs = parser.radio.parseSongSuggestions(suggestionsResponse)
                
                if (songs.isNotEmpty()) {
                    return listOf(
                        Shelf.Lists.Tracks(
                            id = "similar_tracks",
                            title = "Similar Tracks",
                            list = songs,
                            subtitle = "You might also like"
                        )
                    ).toFeed()
                }
            }
            emptyList<Shelf>().toFeed()
        } catch (e: Exception) {
            emptyList<Shelf>().toFeed()
        }
    }
}
