package dev.brahmkshatriya.echo.extension.client

import dev.brahmkshatriya.echo.common.clients.RadioClient
import dev.brahmkshatriya.echo.common.models.*
import dev.brahmkshatriya.echo.common.models.Feed.Companion.toFeed

import dev.brahmkshatriya.echo.extension.*
import dev.brahmkshatriya.echo.extension.utils.*

class RadioClientImpl(
    private val api: JioSaavnApi,
    private val parser: JioSaavnParser
) : RadioClient {

    override suspend fun radio(item: EchoMediaItem, context: EchoMediaItem?): Radio {
        return when (item) {
            is Track -> createRadioFromTrack(item)
            is Album -> createRadioFromAlbum(item)
            is Artist -> createRadioFromArtist(item)
            is Playlist -> createRadioFromPlaylist(item)
            else -> throw Exception("Radio not supported for this item type")
        }
    }
    
    override suspend fun loadTracks(radio: Radio): Feed<Track> {
        val tracksJson = radio.extras["tracks"] ?: return emptyList<Track>().toFeed() as Feed<Track>
        return try {
            val trackIds = tracksJson.split(",")
            val tracks = mutableListOf<Track>()
            
            for (id in trackIds) {
                try {
                    val response = api.getSongDetails(id)
                    val songDetail = parser.parseSongDetails(response).firstOrNull()
                    if (songDetail != null) {
                        tracks.add(songDetailToTrack(songDetail))
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to load track $id for radio: ${e.message}")
                }
            }
            
            tracks.toFeed() as Feed<Track>
        } catch (e: Exception) {
            println("DEBUG: Failed to load radio tracks: ${e.message}")
            emptyList<Track>().toFeed() as Feed<Track>
        }
    }
    
    override suspend fun loadRadio(radio: Radio): Radio = radio
    
    private suspend fun createRadioFromTrack(track: Track): Radio {
        return try {
            println("DEBUG: Creating radio from track: id=${track.id}, title=${track.title}")
            
            try {
                val songId = track.extras["songId"] ?: track.id
                val stationResponse = api.createSongStation(songId)
                val stationId = parser.parseStationId(stationResponse)
                
                if (stationId != null) {
                    println("DEBUG: Created station with ID: $stationId")
        
                    val suggestionsResponse = api.getSongSuggestions(stationId, limit = 50)
                    val songs = parser.parseSongSuggestions(suggestionsResponse)
                    
                    if (songs.isNotEmpty()) {
                        println("DEBUG: Successfully got ${songs.size} song suggestions from station")
                        val trackIds = songs.map { it.id }.joinToString(",")
                        
                        return Radio(
                            id = "radio_${track.id}",
                            title = "${track.title} Radio",
                            subtitle = "Similar to ${track.title}",
                            cover = track.cover,
                            extras = mapOf("tracks" to trackIds)
                        )
                    } else {
                        println("DEBUG: Station created but no suggestions returned")
                    }
                } else {
                    println("DEBUG: Failed to create station, stationId is null")
                }
            } catch (e: Exception) {
                println("DEBUG: Station-based suggestions failed: ${e.message}, falling back to artist radio")
            }
            
            val artists = track.artists
            if (artists.isEmpty()) {
                throw Exception("No artists found for this track")
            }
            
            val primaryArtist = artists.first()
            println("DEBUG: Creating artist-based radio for: ${primaryArtist.name}")
            
            val artistResponse = api.getArtistDetails(primaryArtist.id, songCount = 50, albumCount = 0)
            val artistDetail = parser.parseArtistDetails(artistResponse)
                ?: throw Exception("Could not load artist details")
            
            if (artistDetail.topSongs.isEmpty()) {
                throw Exception("Artist has no songs available")
            }
            
            val trackIds = artistDetail.topSongs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${track.id}",
                title = "${track.title} Radio",
                subtitle = "Songs by ${primaryArtist.name}",
                cover = track.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from track: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }
    
    private suspend fun createRadioFromAlbum(album: Album): Radio {
        return try {
            val response = api.getAlbumDetails(album.id)
            val albumDetail = parser.parseAlbumDetails(response)
                ?: throw Exception("Album not found")
            
            val trackIds = albumDetail.songs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${album.id}",
                title = "${album.title} Radio",
                subtitle = "Songs from ${album.title}",
                cover = album.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from album: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }
    
    internal suspend fun createRadioFromArtist(artist: Artist): Radio {
        return try {
            // Fetch artist details to check radio availability
            val response = api.getArtistDetails(artist.id, songCount = 50, albumCount = 10)
            val artistDetail = parser.parseArtistDetails(response)
                ?: throw Exception("Artist not found")
            
            // Check if radio is available
            if (artistDetail.isRadioPresent) {
                try {
                    // Step 1: Create radio station
                    val language = artistDetail.dominantLanguage.takeIf { it.isNotBlank() } ?: "hindi"
                    val stationResponse = api.createArtistRadioStation(artistDetail.name, language)
                    val stationId = parser.parseArtistRadioStationId(stationResponse)
                    
                    if (stationId != null) {
                        // Step 2: Get radio songs
                        val songsResponse = api.getRadioSongs(stationId, limit = 20)
                        val radioSongs = parser.parseRadioSongs(songsResponse)
                        
                        if (radioSongs.isNotEmpty()) {
                            val trackIds = radioSongs.map { it.id }.joinToString(",")
                            
                            return Radio(
                                id = "radio_${artist.id}",
                                title = "${artist.name} Radio",
                                subtitle = "Radio station for ${artist.name}",
                                cover = artist.cover,
                                extras = mapOf("tracks" to trackIds)
                            )
                        }
                    }
                    
                    // If we reach here, something failed in radio creation
                    println("DEBUG: Radio creation failed for ${artist.name}, falling back to top songs")
                    
                } catch (e: Exception) {
                    println("DEBUG: Radio API failed for ${artist.name}: ${e.message}, falling back to top songs")
                }
            }
            
            // Fallback: Use top songs
            val trackIds = artistDetail.topSongs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${artist.id}",
                title = "${artist.name} Radio",
                subtitle = "Top songs by ${artist.name}",
                cover = artist.cover,
                extras = mapOf("tracks" to trackIds)
            )
            
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from artist: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }

    private suspend fun createRadioFromPlaylist(playlist: Playlist): Radio {
        return try {
            val response = api.getPlaylistDetails(playlist.id)
            val playlistDetail = parser.parsePlaylistDetails(response)
                ?: throw Exception("Playlist not found")
            
            val trackIds = playlistDetail.songs.map { it.id }.joinToString(",")
            
            Radio(
                id = "radio_${playlist.id}",
                title = "${playlist.title} Radio",
                subtitle = "Songs from ${playlist.title}",
                cover = playlist.cover,
                extras = mapOf("tracks" to trackIds)
            )
        } catch (e: Exception) {
            println("DEBUG: Failed to create radio from playlist: ${e.message}")
            throw Exception("Failed to create radio: ${e.message}")
        }
    }

}
