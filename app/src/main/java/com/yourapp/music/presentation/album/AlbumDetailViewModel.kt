package com.yourapp.music.presentation.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.model.Album
import com.yourapp.music.domain.usecase.song.GetAllSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllSongsUseCase: GetAllSongsUseCase
) : ViewModel() {

    private val albumId: String =
        savedStateHandle.get<String>("albumId")
            ?.let { URLDecoder.decode(it, "UTF-8") }
            .orEmpty()

    private val _uiState =
        MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> =
        _uiState.asStateFlow()

    init {
        load()
    }

    /** Fully live — recomputes instantly if the underlying song catalog changes. */
    private fun load() {
        if (albumId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "album_not_found") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllSongsUseCase().collect { allSongs ->
                val songsInAlbum =
                    allSongs.filter { it.album == albumId }
                if (songsInAlbum.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "album_not_found"
                        )
                    }
                    return@collect
                }
                val lastSong =
                    songsInAlbum.last() // same "last added" convention as playlist/album covers elsewhere
                val album = Album(
                    id = albumId,
                    title = albumId,
                    artistName = lastSong.artist,
                    coverUrl = lastSong.albumArtUrl,
                    songIds = songsInAlbum.map { it.id }
                )
                _uiState.update {
                    it.copy(
                        album = album,
                        songs = songsInAlbum,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }
}
