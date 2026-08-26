package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Artist
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    /** Room-cached list the UI observes directly. */
    fun getFollowedArtists(userId: String): Flow<List<Artist>>

    suspend fun followArtist(artistId: String, userId: String): Resource<Unit>
    suspend fun unfollowArtist(artistId: String, userId: String): Resource<Unit>

    /** Pulls followed artists from Supabase and upserts into Room. */
    suspend fun refreshFollowedArtists(userId: String): Resource<Unit>
}
