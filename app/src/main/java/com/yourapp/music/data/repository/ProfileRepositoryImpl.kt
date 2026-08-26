package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.remote.supabase.ProfileApi
import com.yourapp.music.domain.model.UserStats
import com.yourapp.music.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi
) : ProfileRepository {

    override suspend fun getUserStats(userId: String): Resource<UserStats> = try {
        val stats = profileApi.getProfileStats(userId)?.toDomain() ?: UserStats()
        Resource.Success(stats)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't load profile stats", e)
    }

    override suspend fun syncProfileCounts(userId: String, followingCount: Int, playlistCount: Int): Resource<Unit> = try {
        profileApi.upsertProfile(userId, followingCount, playlistCount)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Couldn't sync profile", e)
    }
}
