package com.yourapp.music.presentation.library

import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.model.Song

enum class LibraryTab { ALL, PLAYLISTS, LIKED_SONGS, DOWNLOADS }

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val allSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(), // wired once download/offline caching is built
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
