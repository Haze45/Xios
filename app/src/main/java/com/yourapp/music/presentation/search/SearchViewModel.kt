package com.yourapp.music.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.usecase.search.ClearRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.GetGenresUseCase
import com.yourapp.music.domain.usecase.search.GetRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.SaveRecentSearchUseCase
import com.yourapp.music.domain.usecase.song.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeRecentSearches()
        loadGenres()
        observeSearchResults()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            getRecentSearchesUseCase().collect { recents ->
                _uiState.update { it.copy(recentSearches = recents) }
            }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            getGenresUseCase().collect { genres ->
                _uiState.update { it.copy(genres = genres) }
            }
        }
    }

    /** Debounced live search as the user types — avoids hitting Room on every keystroke. */
    private fun observeSearchResults() {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList()) else searchSongsUseCase(query)
                }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        queryFlow.value = value
    }

    /** Called when the user submits (e.g. taps a recent search or presses search/enter). */
    fun onSearchSubmit(query: String) {
        onQueryChange(query)
        viewModelScope.launch { saveRecentSearchUseCase(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { clearRecentSearchesUseCase() }
    }

    fun onCancelSearch() {
        _uiState.update { it.copy(query = "") }
        queryFlow.value = ""
    }
}
