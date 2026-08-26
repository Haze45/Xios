package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    /** Live, Room-backed — reflects create/add-song/delete instantly, anywhere in the app. */
    fun observeUserPlaylists(userId: String): Flow<List<Playlist>>
    fun observePlaylistById(id: String): Flow<Playlist?>

    suspend fun getTopDailyPlaylists(): Resource<List<Playlist>>
    suspend fun getUserPlaylists(userId: String): Resource<List<Playlist>>
    suspend fun getPlaylistById(id: String): Playlist?
    suspend fun createPlaylist(title: String, ownerId: String): Resource<Playlist>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Resource<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Resource<Unit>
    suspend fun deletePlaylist(playlistId: String): Resource<Unit>
    suspend fun refreshPlaylists(): Resource<Unit>
}
