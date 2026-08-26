package com.yourapp.music.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val albumArtUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val isNewRelease: Boolean = false,
    val isTrending: Boolean = false
)
