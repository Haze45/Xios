package com.yourapp.music.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.playlist.AddSongToPlaylistUseCase
import com.yourapp.music.domain.usecase.playlist.CreatePlaylistUseCase
import com.yourapp.music.domain.usecase.playlist.GetUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.playlist.ObserveUserPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val observeUserPlaylistsUseCase: ObserveUserPlaylistsUseCase,
    private val addSongToPlaylistUseCase: AddSongToPlaylistUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToPlaylistUiState())
    val uiState: StateFlow<AddToPlaylistUiState> = _uiState.asStateFlow()

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    init {
        loadPlaylists()
    }

    /**
     * Hydrates the Room cache from Supabase once, then observes Room live
     * from then on — so this sheet (and every other screen doing the same)
     * reflects add/create/delete the instant it happens, anywhere in the app.
     */
    fun loadPlaylists() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getUserPlaylistsUseCase(uid)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> Unit
            }
            observeUserPlaylistsUseCase(uid).collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun addSongTo(playlistId: String, songId: String) {
        viewModelScope.launch {
            when (val result = addSongToPlaylistUseCase(playlistId, songId)) {
                // No need to manually patch songCount/songIds here anymore —
                // addSongToPlaylistUseCase writes straight to Room, and the
                // observeUserPlaylistsUseCase Flow above picks it up automatically.
                is Resource.Success -> _uiState.update { it.copy(addedTo = it.addedTo + playlistId) }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onShowCreateNew(show: Boolean) {
        _uiState.update { it.copy(isCreatingNew = show, newPlaylistTitle = if (show) it.newPlaylistTitle else "") }
    }

    fun onNewPlaylistTitleChange(value: String) {
        _uiState.update { it.copy(newPlaylistTitle = value) }
    }

    /** Creates a new playlist, then immediately adds the current song to it. */
    fun createPlaylistAndAddSong(songId: String) {
        val uid = userId ?: return
        val title = _uiState.value.newPlaylistTitle
        viewModelScope.launch {
            when (val result = createPlaylistUseCase(title, uid)) {
                is Resource.Success -> {
                    addSongTo(result.data.id, songId)
                    _uiState.update { it.copy(isCreatingNew = false, newPlaylistTitle = "") }
                }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }
}
