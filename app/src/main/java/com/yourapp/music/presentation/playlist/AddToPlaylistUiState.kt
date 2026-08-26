package com.yourapp.music.presentation.playlist

import com.yourapp.music.domain.model.Playlist

data class AddToPlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Playlist IDs the song was just added to in this sheet session — shown with a checkmark. */
    val addedTo: Set<String> = emptySet(),
    val isCreatingNew: Boolean = false,
    val newPlaylistTitle: String = ""
)
