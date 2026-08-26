package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.local.dao.ArtistDao
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.mapper.toEntity
import com.yourapp.music.data.remote.supabase.ArtistApi
import com.yourapp.music.domain.model.Artist
import com.yourapp.music.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first: Room is the source of truth the UI observes. Follow/unfollow
 * writes straight to Supabase then updates the local cache to match — kept
 * simple (no background sync queue) since follow state is low-stakes and
 * rarely conflicts across devices.
 */
@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val artistApi: ArtistApi,
    private val artistDao: ArtistDao
) : ArtistRepository {

    override fun getFollowedArtists(userId: String): Flow<List<Artist>> =
        artistDao.getFollowedArtists(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun followArtist(artistId: String, userId: String): Resource<Unit> = try {
        artistApi.followArtist(userId, artistId)
        refreshFollowedArtists(userId)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't follow artist", e)
    }

    override suspend fun unfollowArtist(artistId: String, userId: String): Resource<Unit> = try {
        artistApi.unfollowArtist(userId, artistId)
        artistDao.removeArtist(artistId, userId)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't unfollow artist", e)
    }

    override suspend fun refreshFollowedArtists(userId: String): Resource<Unit> = try {
        val followIds = artistApi.getFollowedArtistIds(userId).map { it.artist_id }
        val artists = artistApi.getArtistsByIds(followIds)
        artistDao.upsertArtists(artists.map { it.toEntity(userId) })
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't refresh followed artists", e)
    }
}
