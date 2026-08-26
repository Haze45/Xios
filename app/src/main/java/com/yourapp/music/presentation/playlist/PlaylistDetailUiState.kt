package com.yourapp.music.presentation.playlist

import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.model.Song

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
