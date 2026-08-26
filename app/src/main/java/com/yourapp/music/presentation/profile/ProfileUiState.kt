package com.yourapp.music.presentation.profile

import com.yourapp.music.domain.model.Artist
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.model.UserStats

data class ProfileUiState(
    val displayName: String = "",
    val photoUrl: String? = null,
    val stats: UserStats = UserStats(),
    val playlists: List<Playlist> = emptyList(),
    val favoriteArtists: List<Artist> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
