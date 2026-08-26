package com.yourapp.music.presentation.player

import com.yourapp.music.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val isFavorite: Boolean = false
)
