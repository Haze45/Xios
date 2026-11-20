package com.yourapp.music.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.usecase.search.ClearRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.GetGenresUseCase
import com.yourapp.music.domain.usecase.search.GetRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.SaveRecentSearchUseCase
import com.yourapp.music.domain.usecase.song.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val getGenresUseCase: GetGenresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> =
        _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeRecentSearches()
        loadGenres()
        observeSearchResults()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            getRecentSearchesUseCase().collect { recents ->
                _uiState.update {
                    it.copy(recentSearches = recents)
                }
            }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            getGenresUseCase().collect { genres ->
                _uiState.update {
                    it.copy(genres = genres)
                }
            }
        }
    }

    /**
     * Live autocomplete.
     * Debounce avoids re-ranking on every keystroke.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearchResults() {
        viewModelScope.launch {
            queryFlow
                .debounce(200.milliseconds)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        searchSongsUseCase(query.trim())
                    }
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update {
            it.copy(
                query = value,
                searchResults = if (value.isBlank()) {
                    emptyList()
                } else {
                    it.searchResults
                },
                isLoading = value.isNotBlank(),
                errorMessage = null
            )
        }

        queryFlow.value = value
    }

    /** Saves submitted searches without empty history entries. */
    fun onSearchSubmit(query: String) {
        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) return

        onQueryChange(cleanedQuery)

        viewModelScope.launch {
            saveRecentSearchUseCase(cleanedQuery)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            clearRecentSearchesUseCase()
        }
    }

    fun onCancelSearch() {
        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }

        queryFlow.value = ""
    }
}
