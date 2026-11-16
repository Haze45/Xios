package com.yourapp.music.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.album.GetAlbumsUseCase
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.song.GetAllSongsUseCase
import com.yourapp.music.domain.usecase.song.GetNewReleasesUseCase
import com.yourapp.music.domain.usecase.song.RefreshSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val user = getCurrentUserUseCase()
        _uiState.update {
            it.copy(
                userName = user?.displayName ?: user?.email?.substringBefore("@").orEmpty(),
                photoUrl = user?.photoUrl
            )
        }
        loadHome()
        observeCachedSongs()
        observeAlbums()
    }

    /** Room is the source of truth for the song list shown in the UI. */
    private fun observeCachedSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    /** Live: albums are just a grouping of the same cached songs, recomputed whenever that list changes. */
    private fun observeAlbums() {
        viewModelScope.launch {
            getAlbumsUseCase().collect { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Refresh Room cache from Supabase (non-fatal on failure, cache still shown).
            // observeCachedSongs()/observeAlbums() pick up the result automatically.
            when (val result = refreshSongsUseCase()) {
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onFilterSelected(filter: HomeFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        viewModelScope.launch {
            when (filter) {
                HomeFilter.NEW_RELEASE -> applyResource(getNewReleasesUseCase())
                HomeFilter.ALL -> observeCachedSongs()
            }
        }
    }

    private fun applyResource(resource: Resource<List<com.yourapp.music.domain.model.Song>>) {
        when (resource) {
            is Resource.Success -> _uiState.update { it.copy(songs = resource.data, errorMessage = null) }
            is Resource.Error -> _uiState.update { it.copy(errorMessage = resource.message) }
            is Resource.Loading -> Unit
        }
    }
}
