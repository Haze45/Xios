package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.local.dao.SongDao
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.mapper.toEntity
import com.yourapp.music.data.remote.supabase.SongApi
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first: Room is the single source of truth the UI observes.
 * Supabase is only hit to refresh the cache (refreshSongs / getTrending / getNewReleases).
 */
@Singleton
class SongRepositoryImpl @Inject constructor(
    private val songApi: SongApi,
    private val songDao: SongDao
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs()
            .map { list -> list.map { it.toDomain() } }

    override fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getSongById(id: String): Song? =
        songDao.getSongById(id)?.toDomain()

    override suspend fun getTrendingSongs(): Resource<List<Song>> =
        try {
            val songs = songApi.getTrendingSongs()
                .map { it.toDomain() }
            Resource.Success(songs)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "Couldn't load trending songs",
                e
            )
        }

    override suspend fun getNewReleases(): Resource<List<Song>> =
        try {
            val songs = songApi.getNewReleases()
                .map { it.toDomain() }
            Resource.Success(songs)
        } catch (e: Exception) {
            Resource.Error(
                e.message ?: "Couldn't load new releases", e
            )
        }

    override suspend fun refreshSongs(): Resource<Unit> =
        try {
            val remoteSongs = songApi.getAllSongs()
            songDao.upsertSongs(remoteSongs.map { it.toEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            // Non-fatal: UI still has whatever is cached in Room from the last successful sync.
            Resource.Error(
                e.message ?: "Couldn't refresh songs", e
            )
        }
}
