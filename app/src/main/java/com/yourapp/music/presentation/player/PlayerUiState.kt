package com.yourapp.music.presentation.player

import com.yourapp.music.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val isFavorite: Boolean = false,
    /** The current song already has a local copy in Downloads/MusicApp. */
    val isDownloaded: Boolean = false,
    /** A download for the *current* song is in progress right now. */
    val isDownloading: Boolean = false,
    /** One-shot message for the UI (e.g. a Toast); cleared via consumeDownloadMessage(). */
    val downloadMessage: String? = null
)
