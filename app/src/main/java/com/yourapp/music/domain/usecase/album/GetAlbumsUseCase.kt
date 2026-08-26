package com.yourapp.music.domain.usecase.album

import com.yourapp.music.domain.model.Album
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Groups the Room-cached song catalog by album name — live, since it's
 * derived from the same Flow the rest of the app already observes.
 * Songs with a blank album tag are excluded (nothing sensible to group
 * them under).
 */
class GetAlbumsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Album>> = songRepository.getAllSongs().map { songs ->
        songs
            .filter { it.album.isNotBlank() }
            .groupBy { it.album }
            .map { (albumName, songsInAlbum) ->
                val lastSong = songsInAlbum.last() // "last added" — same convention as playlist covers
                Album(
                    id = albumName,
                    title = albumName,
                    artistName = lastSong.artist,
                    coverUrl = lastSong.albumArtUrl,
                    songIds = songsInAlbum.map { it.id }
                )
            }
    }
}
