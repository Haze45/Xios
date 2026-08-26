package com.yourapp.music.domain.model

data class Artist(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isFollowing: Boolean = false
)
