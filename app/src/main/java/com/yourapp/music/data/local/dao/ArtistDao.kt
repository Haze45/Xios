package com.yourapp.music.data.local.dao

import androidx.room.*
import com.yourapp.music.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM followed_artists WHERE userId = :userId")
    fun getFollowedArtists(userId: String): Flow<List<ArtistEntity>>

    @Upsert
    suspend fun upsertArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM followed_artists WHERE id = :artistId AND userId = :userId")
    suspend fun removeArtist(artistId: String, userId: String)

    @Query("DELETE FROM followed_artists WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
