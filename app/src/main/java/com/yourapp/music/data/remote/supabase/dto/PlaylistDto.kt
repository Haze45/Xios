package com.yourapp.music.data.remote.supabase.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: String,
    val title: String,
    val owner_id: String,
    val owner_name: String,
    val cover_url: String,
    val song_count: Int = 0,
    val description: String? = null,
    val song_ids: List<String> = emptyList(),
    val is_top_daily: Boolean = false
)
