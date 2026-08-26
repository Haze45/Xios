package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    /** Single source of truth for the UI — reads straight from Room, never blocks on network. */
    fun getFavoriteSongs(userId: String): Flow<List<Song>>
    fun isFavorite(songId: String, userId: String): Flow<Boolean>

    /** Writes to Room immediately (marked unsynced) and returns; sync happens in the background. */
    suspend fun addFavorite(songId: String, userId: String)
    suspend fun removeFavorite(songId: String, userId: String)

    /** Called by SyncWorker: pushes local unsynced changes, then pulls remote favorites to merge in. */
    suspend fun syncFavorites(userId: String): Resource<Unit>
}
