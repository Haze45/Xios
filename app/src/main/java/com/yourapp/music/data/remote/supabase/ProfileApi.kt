package com.yourapp.music.data.remote.supabase

import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.remote.supabase.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileApi @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getProfileStats(userId: String): ProfileDto? =
        supabase.from(Constants.PROFILES_TABLE).select(Columns.ALL) {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull()

    /**
     * Creates the profiles row on first use, or updates it thereafter.
     * followersCount is intentionally left as whatever it already was
     * (this app has no user-follows-user feature yet, so nothing here
     * ever legitimately computes a new value for it — we just don't want
     * to clobber it back to 0 on every sync of the fields we DO track).
     */
    suspend fun upsertProfile(userId: String, followingCount: Int, playlistCount: Int) {
        val existingFollowers = getProfileStats(userId)?.followers_count ?: 0
        supabase.from(Constants.PROFILES_TABLE).upsert(
            ProfileDto(
                user_id = userId,
                following_count = followingCount,
                followers_count = existingFollowers,
                playlist_count = playlistCount
            )
        )
    }

    /**
     * Uploads a profile photo to Storage and returns its public URL.
     * Keyed by userId so re-uploading always overwrites the same object
     * (upsert = true) instead of piling up orphaned old photos.
     */
    suspend fun uploadProfilePhoto(userId: String, bytes: ByteArray): String {
        val bucket = supabase.storage.from(Constants.PROFILE_PHOTOS_BUCKET)
        val path = "$userId.jpg"
        bucket.upload(path, bytes, upsert = true)
        return bucket.publicUrl(path)
    }
}
