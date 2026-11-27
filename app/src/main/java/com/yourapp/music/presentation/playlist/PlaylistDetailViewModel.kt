package com.yourapp.music.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.playlist.ObservePlaylistByIdUseCase
import com.yourapp.music.domain.usecase.playlist.RemoveSongFromPlaylistUseCase
import com.yourapp.music.domain.usecase.song.GetAllSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observePlaylistByIdUseCase: ObservePlaylistByIdUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val removeSongFromPlaylistUseCase: RemoveSongFromPlaylistUseCase
) : ViewModel() {

    private val playlistId: String =
        savedStateHandle.get<String>("playlistId").orEmpty()

    private val _uiState =
        MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> =
        _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Fully live: combines the playlist itself (Room-backed) with the
     * cached song list, so if a song is added to THIS playlist from
     * anywhere else in the app — e.g. the Player screen's Add to Playlist
     * sheet — while this screen happens to already be open, both the
     * header's song count and the visible song list update immediately,
     * with no manual refresh needed.
     */
    private fun load() {
        if (playlistId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "playlist_not_found") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                observePlaylistByIdUseCase(playlistId),
                getAllSongsUseCase()
            ) { playlist, allSongs ->
                playlist to allSongs
            }.collect { (playlist, allSongs) ->
                if (playlist == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "playlist_not_found"
                        )
                    }
                    return@collect
                }
                val songsInPlaylist =
                    playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
                _uiState.update {
                    it.copy(
                        playlist = playlist,
                        songs = songsInPlaylist,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            val id = playlistId
            if (id.isBlank()) return@launch
            when (removeSongFromPlaylistUseCase(id, songId)) {
                is Resource.Error -> _uiState.update {
                    it.copy(
                        errorMessage = "error_generic"
                    )
                }

                else -> Unit
            }
        }
    }
}
