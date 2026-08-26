package com.yourapp.music.presentation.queue

import com.yourapp.music.domain.model.Song

data class QueueUiState(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false
)
