package com.yourapp.music.core.player

import com.yourapp.music.domain.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val isFavorite: Boolean = false
)
