package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.local.dao.FavoriteDao
import com.yourapp.music.data.local.entity.FavoriteEntity
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.mapper.toDto
import com.yourapp.music.data.mapper.toEntity
import com.yourapp.music.data.remote.supabase.FavoriteApi
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first: the UI only ever reads from Room (getFavoriteSongs / isFavorite),
 * so add/remove feel instant. syncFavorites() is called by SyncWorker in the
 * background to reconcile local changes with Supabase and pull down any
 * favorites added on another device.
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteApi: FavoriteApi,
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavoriteSongs(userId: String): Flow<List<Song>> =
        favoriteDao.getFavoriteSongs(userId).map { list -> list.map { it.toDomain() } }

    override fun isFavorite(songId: String, userId: String): Flow<Boolean> =
        favoriteDao.isFavorite(songId, userId)

    override suspend fun addFavorite(songId: String, userId: String) {
        favoriteDao.upsertFavorite(
            FavoriteEntity(
                songId = songId,
                userId = userId,
                addedAt = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            )
        )
    }

    override suspend fun removeFavorite(songId: String, userId: String) {
        favoriteDao.markDeleted(songId, userId)
    }

    override suspend fun syncFavorites(userId: String): Resource<Unit> = try {
        // 1. Push local unsynced changes (additions and soft-deletes) up to Supabase.
        val unsynced = favoriteDao.getUnsyncedFavorites()
        unsynced.forEach { local ->
            if (local.isDeleted) {
                favoriteApi.deleteFavorite(local.songId, local.userId)
            } else {
                favoriteApi.upsertFavorite(local.toDto())
            }
            favoriteDao.markSynced(local.songId, local.userId)
        }

        // 2. Pull remote favorites down so favorites added on another device show up here.
        val remote = favoriteApi.getFavorites(userId)
        favoriteDao.upsertFavorites(remote.map { it.toEntity(isSynced = true) })

        Resource.Success(Unit)
    } catch (e: Exception) {
        // Non-fatal: local Room state is untouched, next sync attempt will retry.
        Resource.Error(e.message ?: "Couldn't sync favorites", e)
    }
}
