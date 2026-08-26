package com.yourapp.music.domain.model

data class Favorite(
    val songId: String,
    val userId: String,
    val addedAt: Long
)
