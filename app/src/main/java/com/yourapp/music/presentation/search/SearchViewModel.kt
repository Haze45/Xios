package com.yourapp.music.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.usecase.search.ClearRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.GetGenresUseCase
import com.yourapp.music.domain.usecase.search.GetRecentSearchesUseCase
import com.yourapp.music.domain.usecase.search.SaveRecentSearchUseCase
import com.yourapp.music.domain.usecase.song.HybridSearchSongsUseCase
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
    private val hybridSearchSongsUseCase: HybridSearchSongsUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
    private val getGenresUseCase: GetGenresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> =
        _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    private var hybridSearchJob: Job? = null

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
     * No Gemini request is made while the user types.
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
                    // Do not overwrite submitted hybrid results.
                    if (!_uiState.value.isHybridSearch) {
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
        hybridSearchJob?.cancel()
        hybridSearchJob = null

        _uiState.update {
            it.copy(
                query = value,
                searchResults = if (value.isBlank()) {
                    emptyList()
                } else {
                    it.searchResults
                },
                isLoading = value.isNotBlank(),
                isHybridSearch = false,
                isLocalFallback = false,
                errorMessage = null
            )
        }

        queryFlow.value = value
    }

    /**
     * Submits a query for exact, fuzzy and semantic hybrid ranking.
     */
    fun onSearchSubmit(query: String) {
        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) return

        onQueryChange(cleanedQuery)

        _uiState.update {
            it.copy(
                isHybridSearch = true,
                isLoading = true,
                isLocalFallback = false,
                errorMessage = null
            )
        }

        saveRecentSearch(cleanedQuery)

        hybridSearchJob = viewModelScope.launch {
            try {
                val result =
                    hybridSearchSongsUseCase(cleanedQuery)

                _uiState.update { current ->
                    if (
                        current.query == cleanedQuery &&
                        current.isHybridSearch
                    ) {
                        current.copy(
                            searchResults = result.songs,
                            isLoading = false,
                            isLocalFallback =
                                !result.semanticSearchSucceeded,
                            errorMessage = if (
                                result.semanticSearchSucceeded
                            ) {
                                null
                            } else {
                                "search_semantic_fallback"
                            }
                        )
                    } else {
                        current
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { current ->
                    if (
                        current.query == cleanedQuery &&
                        current.isHybridSearch
                    ) {
                        current.copy(
                            isLoading = false,
                            isLocalFallback = true,
                            errorMessage =
                                "search_failed"
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

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
        hybridSearchJob?.cancel()
        hybridSearchJob = null

        _uiState.update {
            it.copy(
                query = "",
                searchResults = emptyList(),
                isLoading = false,
                isHybridSearch = false,
                isLocalFallback = false,
                errorMessage = null
            )
        }

        queryFlow.value = ""
    }
}
