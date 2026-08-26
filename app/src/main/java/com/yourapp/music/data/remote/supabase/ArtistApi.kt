package com.yourapp.music.data.remote.supabase

import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.remote.supabase.dto.ArtistDto
import com.yourapp.music.data.remote.supabase.dto.ArtistFollowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistApi @Inject constructor(
    private val supabase: SupabaseClient
) {
    /** artist_follows rows for this user (just the relationship, no artist details yet). */
    suspend fun getFollowedArtistIds(userId: String): List<ArtistFollowDto> =
        supabase.from(Constants.ARTIST_FOLLOWS_TABLE).select(Columns.ALL) {
            filter { eq("user_id", userId) }
        }.decodeList()

    suspend fun getArtistsByIds(ids: List<String>): List<ArtistDto> {
        if (ids.isEmpty()) return emptyList()
        return supabase.from(Constants.ARTISTS_TABLE).select(Columns.ALL) {
            filter { isIn("id", ids) }
        }.decodeList()
    }

    suspend fun followArtist(userId: String, artistId: String) {
        supabase.from(Constants.ARTIST_FOLLOWS_TABLE).upsert(
            ArtistFollowDto(user_id = userId, artist_id = artistId)
        )
    }

    suspend fun unfollowArtist(userId: String, artistId: String) {
        supabase.from(Constants.ARTIST_FOLLOWS_TABLE).delete {
            filter {
                eq("user_id", userId)
                eq("artist_id", artistId)
            }
        }
    }
}
