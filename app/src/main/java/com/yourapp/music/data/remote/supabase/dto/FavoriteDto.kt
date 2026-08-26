package com.yourapp.music.data.remote.supabase.dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
    val song_id: String,
    val user_id: String,
    val added_at: Long
)
