package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.extension.parser.*

class JioSaavnParser {
    val track = TrackParser()
    val album = AlbumParser(track)
    val artist = ArtistParser(track, album)
    val playlist = PlaylistParser(track)
    val search = SearchParser(track, album, artist, playlist)
    val home = HomeParser(track, album, playlist)
    val radio = RadioParser(track)
}
