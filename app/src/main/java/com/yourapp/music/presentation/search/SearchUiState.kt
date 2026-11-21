package com.yourapp.music.presentation.search

import com.yourapp.music.domain.model.Genre
import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.model.Song

data class SearchUiState(
    val query: String = "",
    val searchResults: List<Song> = emptyList(),
    val recentSearches: List<RecentSearch> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val isLoading: Boolean = false,
    val isHybridSearch: Boolean = false,
    val isLocalFallback: Boolean = false,
    val errorMessage: String? = null
) {
    val isSearchActive: Boolean
        get() = query.isNotBlank()
}
