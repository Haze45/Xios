package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.UserStats

interface ProfileRepository {
    suspend fun getUserStats(userId: String): Resource<UserStats>

    /** Writes the counts this app actually tracks locally (following artists, playlists) back to Supabase. */
    suspend fun syncProfileCounts(userId: String, followingCount: Int, playlistCount: Int): Resource<Unit>

    /** Uploads a photo to Storage and returns its public URL — needed so the photo is visible on every device, not just the one it was picked on. */
    suspend fun uploadProfilePhoto(userId: String, bytes: ByteArray): Resource<String>
}
