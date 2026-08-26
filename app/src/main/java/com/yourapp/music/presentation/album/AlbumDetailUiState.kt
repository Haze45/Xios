package com.yourapp.music.presentation.album

import com.yourapp.music.domain.model.Album
import com.yourapp.music.domain.model.Song

data class AlbumDetailUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
