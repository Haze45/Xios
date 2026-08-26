package com.yourapp.music.data.remote.supabase.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val user_id: String,
    val following_count: Int = 0,
    val followers_count: Int = 0,
    val playlist_count: Int = 0
)
