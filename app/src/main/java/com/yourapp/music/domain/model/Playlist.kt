package com.yourapp.music.domain.model

data class Playlist(
    val id: String,
    val title: String,
    val ownerId: String = "",
    val ownerName: String,
    val coverUrl: String,
    val songCount: Int,
    val description: String? = null,
    val songIds: List<String> = emptyList()
)
