package com.yourapp.music.data.remote.supabase

import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.remote.supabase.dto.SongDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongApi @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getAllSongs(): List<SongDto> =
        supabase.from(Constants.SONGS_TABLE).select(Columns.ALL).decodeList()

    suspend fun getTrendingSongs(): List<SongDto> =
        supabase.from(Constants.SONGS_TABLE).select(Columns.ALL) {
            filter { eq("is_trending", true) }
        }.decodeList()

    suspend fun getNewReleases(): List<SongDto> =
        supabase.from(Constants.SONGS_TABLE).select(Columns.ALL) {
            filter { eq("is_new_release", true) }
        }.decodeList()
}
