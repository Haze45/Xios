package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    /** Room is the single source of truth for the UI; this Flow updates the moment cache changes. */
    fun getAllSongs(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun getSongById(id: String): Song?
    suspend fun getTrendingSongs(): Resource<List<Song>>
    suspend fun getNewReleases(): Resource<List<Song>>

    /** Pulls fresh data from Supabase and upserts into Room. Call on screen load / pull-to-refresh. */
    suspend fun refreshSongs(): Resource<Unit>
}
