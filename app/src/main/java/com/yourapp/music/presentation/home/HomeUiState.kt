package com.yourapp.music.presentation.home

import com.yourapp.music.domain.model.Album
import com.yourapp.music.domain.model.Song

enum class HomeFilter { ALL, NEW_RELEASE }

data class HomeUiState(
    val userName: String = "",
    val photoUrl: String? = null,
    val selectedFilter: HomeFilter = HomeFilter.ALL,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
