package com.yourapp.music.data.local.dao

import androidx.room.*
import com.yourapp.music.data.local.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE ownerId = :ownerId")
    fun getPlaylistsByOwner(ownerId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistByIdFlow(playlistId: String): Flow<PlaylistEntity?>

    @Upsert
    suspend fun upsertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)
}
