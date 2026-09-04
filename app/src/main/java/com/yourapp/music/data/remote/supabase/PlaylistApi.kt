package com.yourapp.music.data.remote.supabase

import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.remote.supabase.dto.PlaylistDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistApi @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getAllPlaylists(): List<PlaylistDto> =
        supabase.from(Constants.PLAYLISTS_TABLE).select(Columns.ALL).decodeList()

    suspend fun getTopDailyPlaylists(): List<PlaylistDto> =
        supabase.from(Constants.PLAYLISTS_TABLE).select(Columns.ALL) {
            filter { eq("is_top_daily", true) }
        }.decodeList()

    suspend fun getUserPlaylists(userId: String): List<PlaylistDto> =
        supabase.from(Constants.PLAYLISTS_TABLE).select(Columns.ALL) {
            filter { eq("owner_id", userId) }
        }.decodeList()

    suspend fun getPlaylistById(playlistId: String): PlaylistDto? =
        supabase.from(Constants.PLAYLISTS_TABLE).select(Columns.ALL) {
            filter { eq("id", playlistId) }
        }.decodeSingleOrNull()

    suspend fun createPlaylist(playlist: PlaylistDto): PlaylistDto =
        supabase.from(Constants.PLAYLISTS_TABLE).insert(playlist) { select() }.decodeSingle()

    suspend fun updatePlaylistSongIds(playlistId: String, songIds: List<String>, songCount: Int, coverUrl: String) {
        supabase.from(Constants.PLAYLISTS_TABLE).update({
            set("song_ids", songIds)
            set("song_count", songCount)
            set("cover_url", coverUrl)
        }) {
            filter { eq("id", playlistId) }
        }
    }

    suspend fun renamePlaylist(playlistId: String, newTitle: String) {
        supabase.from(Constants.PLAYLISTS_TABLE).update({
            set("title", newTitle)
        }) {
            filter { eq("id", playlistId) }
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        supabase.from(Constants.PLAYLISTS_TABLE).delete {
            filter { eq("id", playlistId) }
        }
    }
}
