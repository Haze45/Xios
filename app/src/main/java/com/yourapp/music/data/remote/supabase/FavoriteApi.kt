package com.yourapp.music.data.remote.supabase

import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.remote.supabase.dto.FavoriteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteApi @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getFavorites(userId: String): List<FavoriteDto> =
        supabase.from(Constants.FAVORITES_TABLE).select(Columns.ALL) {
            filter { eq("user_id", userId) }
        }.decodeList()

    suspend fun upsertFavorite(favorite: FavoriteDto) {
        supabase.from(Constants.FAVORITES_TABLE).upsert(favorite)
    }

    suspend fun deleteFavorite(songId: String, userId: String) {
        supabase.from(Constants.FAVORITES_TABLE).delete {
            filter {
                eq("song_id", songId)
                eq("user_id", userId)
            }
        }
    }
}
