package com.yourapp.music.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.album.GetAlbumsUseCase
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.song.GetAllSongsUseCase
import com.yourapp.music.domain.usecase.song.GetNewReleasesUseCase
import com.yourapp.music.domain.usecase.song.GetPersonalizedRecommendationsUseCase
import com.yourapp.music.domain.usecase.song.RefreshSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val getNewReleasesUseCase: GetNewReleasesUseCase,
    private val refreshSongsUseCase: RefreshSongsUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getPersonalizedRecommendationsUseCase:
    GetPersonalizedRecommendationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private var recommendationJob: Job? = null

    init {
        observeCurrentUser()
        loadHome()
        observeCachedSongs()
        observeAlbums()
        refreshPersonalizedRecommendations()
    }

    /** Room is the source of truth for the song list shown in the UI. */
    private fun observeCachedSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _uiState.update {
                    it.copy(songs = songs)
                }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase.observe()
                .collect { user ->
                    _uiState.update {
                        it.copy(
                            userName = user?.displayName
                                ?: user?.email?.substringBefore(
                                    "@"
                                ).orEmpty(),
                            photoUrl = user?.photoUrl
                        )
                    }
                }
        }
    }

    /** Albums are grouped from the cached songs. */
    private fun observeAlbums() {
        viewModelScope.launch {
            getAlbumsUseCase().collect { albums ->
                _uiState.update {
                    it.copy(albums = albums)
                }
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            // Refresh Room cache from Supabase.
            // Cached data remains available if the network fails.
            when (val result = refreshSongsUseCase()) {
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }

                else -> Unit
            }

            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }

    /**
     * Generate personalized recommendations using the user's favorites.
     */
    fun refreshPersonalizedRecommendations() {
        recommendationJob?.cancel()

        recommendationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecommendationsLoading = true,
                    recommendationMessage = null
                )
            }

            try {
                val recommendations =
                    getPersonalizedRecommendationsUseCase()

                _uiState.update {
                    it.copy(
                        recommendedSongs = recommendations,
                        isRecommendationsLoading = false,
                        recommendationMessage =
                            if (recommendations.isEmpty()) {
                                "Like a few songs to discover music you'll enjoy."
                            } else {
                                null
                            }
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isRecommendationsLoading = false,
                        recommendationMessage =
                            "Couldn't load your recommendations. Tap Refresh to try again."
                    )
                }
            }
        }
    }

    fun onFilterSelected(filter: HomeFilter) {
        _uiState.update {
            it.copy(selectedFilter = filter)
        }

        viewModelScope.launch {
            when (filter) {
                HomeFilter.NEW_RELEASE -> {
                    applyResource(getNewReleasesUseCase())
                }

                HomeFilter.ALL -> {
                    observeCachedSongs()
                }
            }
        }
    }

    private fun applyResource(
        resource: Resource<List<com.yourapp.music.domain.model.Song>>
    ) {
        when (resource) {
            is Resource.Success -> {
                _uiState.update {
                    it.copy(
                        songs = resource.data,
                        errorMessage = null
                    )
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(errorMessage = resource.message)
                }
            }

            is Resource.Loading -> Unit
        }
    }
}
