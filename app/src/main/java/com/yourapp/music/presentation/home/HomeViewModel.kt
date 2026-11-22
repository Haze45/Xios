package com.yourapp.music.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.ListeningHistoryRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    GetPersonalizedRecommendationsUseCase,
    private val listeningHistoryRepository: ListeningHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private var recommendationJob: Job? = null
    private var listeningHistoryJob: Job? = null

    init {
        observeCurrentUser()
        observeCachedSongs()
        observeAlbums()
        loadHome()
    }

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
                            photoUrl = user?.photoUrl,
                            recentlyPlayedSongs = if (user == null) {
                                emptyList()
                            } else {
                                it.recentlyPlayedSongs
                            },
                            recommendedSongs = if (user == null) {
                                emptyList()
                            } else {
                                it.recommendedSongs
                            }
                        )
                    }

                    listeningHistoryJob?.cancel()

                    if (user == null) {
                        _uiState.update {
                            it.copy(
                                recentlyPlayedSongs = emptyList(),
                                recommendedSongs = emptyList(),
                                recommendationMessage = null
                            )
                        }
                    } else {
                        listeningHistoryJob =
                            viewModelScope.launch {
                                combine(
                                    listeningHistoryRepository.observeHistory(
                                        userId = user.uid,
                                        limit = 30
                                    ),
                                    getAllSongsUseCase()
                                ) { history, cachedSongs ->
                                    val songsById =
                                        cachedSongs.associateBy {
                                            it.id
                                        }

                                    history
                                        .mapNotNull { entry ->
                                            songsById[entry.songId]
                                        }
                                        .distinctBy { it.id }
                                        .take(10)
                                }.collect { recentlyPlayed ->
                                    _uiState.update {
                                        it.copy(
                                            recentlyPlayedSongs = recentlyPlayed
                                        )
                                    }
                                }
                            }

                        // Cache-aware recommendation load for this user.
                        loadRecommendations(forceRefresh = false)
                    }
                }
        }
    }

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

            // Room has had a chance to receive the refreshed songs.
            loadRecommendations(forceRefresh = false)
        }
    }

    /**
     * Manual refresh bypasses the 24-hour recommendation cache.
     */
    fun refreshPersonalizedRecommendations() {
        loadRecommendations(forceRefresh = true)
    }

    private fun loadRecommendations(forceRefresh: Boolean) {
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
                    getPersonalizedRecommendationsUseCase(
                        forceRefresh = forceRefresh
                    )

                _uiState.update {
                    it.copy(
                        recommendedSongs = recommendations,
                        isRecommendationsLoading = false,
                        recommendationMessage =
                            if (recommendations.isEmpty()) {
                                "Like or listen to a few songs to get personalized recommendations."
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
                            "Couldn't load recommendations. Tap Refresh to try again."
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
                    val songs = getAllSongsUseCase().first()
                    _uiState.update {
                        it.copy(songs = songs)
                    }
                }
            }
        }
    }

    private fun applyResource(resource: Resource<List<Song>>) {
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
