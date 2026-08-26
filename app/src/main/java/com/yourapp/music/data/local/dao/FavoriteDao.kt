package com.yourapp.music.data.local.dao

import androidx.room.*
import com.yourapp.music.data.local.entity.FavoriteEntity
import com.yourapp.music.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId AND isDeleted = 0")
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>>

    /** Joins favorites with the cached songs table so the UI gets full Song objects directly. */
    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN favorites ON songs.id = favorites.songId
        WHERE favorites.userId = :userId AND favorites.isDeleted = 0
        ORDER BY favorites.addedAt DESC
        """
    )
    fun getFavoriteSongs(userId: String): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId AND userId = :userId AND isDeleted = 0)")
    fun isFavorite(songId: String, userId: String): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE isSynced = 0")
    suspend fun getUnsyncedFavorites(): List<FavoriteEntity>

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteEntity)

    @Upsert
    suspend fun upsertFavorites(favorites: List<FavoriteEntity>)

    @Query("UPDATE favorites SET isDeleted = 1, isSynced = 0 WHERE songId = :songId AND userId = :userId")
    suspend fun markDeleted(songId: String, userId: String)

    @Query("UPDATE favorites SET isSynced = 1 WHERE songId = :songId AND userId = :userId")
    suspend fun markSynced(songId: String, userId: String)
}
