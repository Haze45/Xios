package com.yourapp.music.data.remote.supabase.dto

import kotlinx.serialization.Serializable

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val album_art_url: String,
    val audio_url: String,
    val duration_ms: Long,
    val is_new_release: Boolean = false,
    val is_trending: Boolean = false
)
