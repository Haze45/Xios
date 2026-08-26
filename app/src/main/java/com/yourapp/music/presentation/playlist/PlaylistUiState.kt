package com.yourapp.music.presentation.playlist

import com.yourapp.music.domain.model.Playlist

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newPlaylistTitle: String = ""
)
