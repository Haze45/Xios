package com.yourapp.music.presentation.favorites

import com.yourapp.music.domain.model.Song

data class FavoritesUiState(
    val songs: List<Song> = emptyList()
)
