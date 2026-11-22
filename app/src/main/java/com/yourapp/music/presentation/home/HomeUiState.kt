package com.yourapp.music.presentation.home

import com.yourapp.music.domain.model.Album
import com.yourapp.music.domain.model.Song

enum class HomeFilter {
    ALL,
    NEW_RELEASE
}

data class HomeUiState(
    val userName: String = "",
    val photoUrl: String? = null,
    val selectedFilter: HomeFilter = HomeFilter.ALL,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),

    // Personalized recommendations
    val recommendedSongs: List<Song> = emptyList(),
    val isRecommendationsLoading: Boolean = false,
    val recommendationMessage: String? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
