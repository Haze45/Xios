package com.yourapp.music.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.playlist.CreatePlaylistUseCase
import com.yourapp.music.domain.usecase.playlist.DeletePlaylistUseCase
import com.yourapp.music.domain.usecase.playlist.GetUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.playlist.ObserveUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.playlist.RenamePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val observeUserPlaylistsUseCase: ObserveUserPlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    init {
        loadPlaylists()
        observeLivePlaylists()
    }

    /** One-shot: pulls fresh data from Supabase into Room (also fine to call again for pull-to-refresh). */
    fun loadPlaylists() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getUserPlaylistsUseCase(uid)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    /** Live: Room updates instantly whenever a song is added/removed or a playlist is created/deleted anywhere in the app. */
    private fun observeLivePlaylists() {
        val uid = userId ?: return
        viewModelScope.launch {
            observeUserPlaylistsUseCase(uid).collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun onShowCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show, newPlaylistTitle = if (show) it.newPlaylistTitle else "") }
    }

    fun onNewPlaylistTitleChange(value: String) {
        _uiState.update { it.copy(newPlaylistTitle = value) }
    }

    fun createPlaylist() {
        val uid = userId ?: return
        val title = _uiState.value.newPlaylistTitle
        viewModelScope.launch {
            when (val result = createPlaylistUseCase(title, uid)) {
                // No manual loadPlaylists() re-fetch needed anymore — the live
                // Flow above will show the new playlist automatically since
                // createPlaylistUseCase writes straight to Room.
                is Resource.Success -> _uiState.update { it.copy(showCreateDialog = false, newPlaylistTitle = "") }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    // ---- Kebab menu (PlaylistOptionsSheet) ----

    /** Tapping a row's kebab (⋮) icon opens the options sheet for that playlist. */
    fun onShowOptions(playlist: Playlist) {
        _uiState.update { it.copy(playlistForOptions = playlist) }
    }

    fun onDismissOptions() {
        _uiState.update { it.copy(playlistForOptions = null) }
    }

    /** "Rename" tapped inside the options sheet — close the sheet, open the rename dialog pre-filled with the current title. */
    fun onRequestRename() {
        val playlist = _uiState.value.playlistForOptions ?: return
        _uiState.update {
            it.copy(playlistForOptions = null, playlistBeingRenamed = playlist, renameTitle = playlist.title)
        }
    }

    /** "Delete" tapped inside the options sheet — close the sheet, open the delete confirmation dialog. */
    fun onRequestDelete() {
        val playlist = _uiState.value.playlistForOptions ?: return
        _uiState.update { it.copy(playlistForOptions = null, playlistPendingDelete = playlist) }
    }

    // ---- Rename dialog ----

    fun onRenameTitleChange(value: String) {
        _uiState.update { it.copy(renameTitle = value) }
    }

    fun onCancelRename() {
        _uiState.update { it.copy(playlistBeingRenamed = null, renameTitle = "") }
    }

    fun confirmRename() {
        val playlist = _uiState.value.playlistBeingRenamed ?: return
        val newTitle = _uiState.value.renameTitle
        viewModelScope.launch {
            when (val result = renamePlaylistUseCase(playlist.id, newTitle)) {
                // No manual re-fetch needed — the live Flow above reflects
                // the rename automatically since it writes straight to Room.
                is Resource.Success -> _uiState.update { it.copy(playlistBeingRenamed = null, renameTitle = "") }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    // ---- Delete confirmation dialog ----

    fun onCancelDelete() {
        _uiState.update { it.copy(playlistPendingDelete = null) }
    }

    fun confirmDelete() {
        val playlist = _uiState.value.playlistPendingDelete ?: return
        viewModelScope.launch {
            when (val result = deletePlaylistUseCase(playlist.id)) {
                // No manual re-fetch needed — the live Flow above reflects
                // the deletion automatically since it writes straight to Room.
                is Resource.Success -> _uiState.update { it.copy(playlistPendingDelete = null) }
                is Resource.Error -> _uiState.update { it.copy(errorMessage = result.message, playlistPendingDelete = null) }
                is Resource.Loading -> Unit
            }
        }
    }
}
