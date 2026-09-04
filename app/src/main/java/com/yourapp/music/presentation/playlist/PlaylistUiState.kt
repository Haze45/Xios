package com.yourapp.music.presentation.playlist

import com.yourapp.music.domain.model.Playlist

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val newPlaylistTitle: String = "",

    /** Set when a row's kebab (⋮) menu is tapped — shows PlaylistOptionsSheet for this playlist. */
    val playlistForOptions: Playlist? = null,

    /** Set when "Rename" is chosen from the options sheet — shows the rename dialog. */
    val playlistBeingRenamed: Playlist? = null,
    val renameTitle: String = "",

    /** Set when "Delete" is chosen from the options sheet — shows the delete confirmation dialog. */
    val playlistPendingDelete: Playlist? = null
)
