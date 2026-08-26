package com.yourapp.music.data.remote.supabase.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistFollowDto(
    val user_id: String,
    val artist_id: String,
    val followed_at: Long = System.currentTimeMillis()
)
