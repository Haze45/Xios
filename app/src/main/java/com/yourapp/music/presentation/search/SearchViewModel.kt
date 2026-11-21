package com.yourapp.music.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.usecase.search.ClearRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.GetGenresUseCase
import com.yourapp.music.domain.usecase.search.GetRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.SaveRecentSearchUseCase
import com.yourapp.music.domain.usecase.search.SemanticSearchSongsUseCase
import com.yourapp.music.domain.usecase.song.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    private val semanticSearchSongsUseCase: SemanticSearchSongsUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val getGenresUseCase: GetGenresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> =
        _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    private var semanticSearchJob: Job? = null

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
     * Live local autocomplete.
     * This does not call Gemini.
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
                    // Do not overwrite results from an active semantic search.
                    if (!_uiState.value.isSemanticSearch) {
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
    }

    fun onQueryChange(value: String) {
        semanticSearchJob?.cancel()
        semanticSearchJob = null

        _uiState.update {
            it.copy(
                query = value,
                searchResults = if (value.isBlank()) {
                    emptyList()
                } else {
                    it.searchResults
                },
                isLoading = value.isNotBlank(),
                isSemanticSearch = false,
                errorMessage = null
            )
        }

        queryFlow.value = value
    }

    /**
     * Runs semantic search only after explicit submission.
     */
    fun onSearchSubmit(query: String) {
        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) return

        onQueryChange(cleanedQuery)

        _uiState.update {
            it.copy(
                isSemanticSearch = true,
                isLoading = true,
                errorMessage = null
            )
        }

        saveRecentSearch(cleanedQuery)

        semanticSearchJob = viewModelScope.launch {
            try {
                val results = semanticSearchSongsUseCase(
                    query = cleanedQuery,
                    matchCount = 20
                )

                _uiState.update { current ->
                    if (
                        current.query == cleanedQuery &&
                        current.isSemanticSearch
                    ) {
                        current.copy(
                            searchResults = results,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        current
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { current ->
                    if (
                        current.query == cleanedQuery &&
                        current.isSemanticSearch
                    ) {
                        current.copy(
                            isLoading = false,
                            errorMessage =
                                "Semantic search is unavailable. Showing local matches."
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    /**
     * Saves a query without launching a semantic request.
     * Used when the user selects a song.
     */
    fun saveRecentSearch(query: String) {
        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) return

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
        semanticSearchJob?.cancel()
        semanticSearchJob = null

        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                isLoading = false,
                isSemanticSearch = false,
                errorMessage = null
            )
        }

        queryFlow.value = ""
    }
}
