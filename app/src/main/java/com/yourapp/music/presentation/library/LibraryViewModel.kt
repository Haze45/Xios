package com.yourapp.music.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.download.DownloadWorkManager
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.download.GetDownloadedSongsUseCase
import com.yourapp.music.domain.usecase.download.ObserveDownloadProgressUseCase
import com.yourapp.music.domain.usecase.favorite.AddFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.GetFavoritesUseCase
import com.yourapp.music.domain.usecase.favorite.RemoveFavoriteUseCase
import com.yourapp.music.domain.usecase.playlist.GetUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.playlist.ObserveUserPlaylistsUseCase
import com.yourapp.music.domain.usecase.song.GetAllSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val getDownloadedSongsUseCase: GetDownloadedSongsUseCase,
    private val observeDownloadProgressUseCase: ObserveDownloadProgressUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val observeUserPlaylistsUseCase: ObserveUserPlaylistsUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val favoriteSyncManager: FavoriteSyncManager,
    private val downloadWorkManager: DownloadWorkManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> =
        _uiState.asStateFlow()

    private val userId: String?
        get() = getCurrentUserUseCase()?.uid

    init {
        observeAllSongs()
        observeDownloadedSongs()
        observeDownloadProgress()
        observeLikedSongs()
        loadPlaylists()
        favoriteSyncManager.enqueueSync()
    }

    private fun observeDownloadedSongs() {
        viewModelScope.launch {
            getDownloadedSongsUseCase().collect { songs ->
                _uiState.update {
                    it.copy(downloadedSongs = songs)
                }
            }
        }
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            observeDownloadProgressUseCase().collect { progress ->
                _uiState.update {
                    it.copy(downloadingSongs = progress)
                }
            }
        }
    }

    fun onTabSelected(tab: LibraryTab) {
        _uiState.update {
            it.copy(selectedTab = tab)
        }
    }

    private fun observeAllSongs() {
        viewModelScope.launch {
            getAllSongsUseCase().collect { songs ->
                _uiState.update {
                    it.copy(allSongs = songs)
                }
            }
        }
    }

    private fun observeLikedSongs() {
        val uid = userId ?: return

        viewModelScope.launch {
            getFavoritesUseCase(uid).collect { songs ->
                _uiState.update {
                    it.copy(likedSongs = songs)
                }
            }
        }
    }

    private fun loadPlaylists() {
        val uid = userId ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }

            when (val result =
                getUserPlaylistsUseCase(uid)) {
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
            }

            observeUserPlaylistsUseCase(uid).collect { playlists ->
                _uiState.update {
                    it.copy(playlists = playlists)
                }
            }
        }
    }

    fun toggleLike(
        songId: String,
        isCurrentlyLiked: Boolean
    ) {
        val uid = userId ?: return

        viewModelScope.launch {
            if (isCurrentlyLiked) {
                removeFavoriteUseCase(songId, uid)
            } else {
                addFavoriteUseCase(songId, uid)
            }

            favoriteSyncManager.enqueueSync()
        }
    }

    fun pauseDownload(songId: String) {
        viewModelScope.launch {
            downloadWorkManager.pauseDownload(songId)
        }
    }

    fun resumeDownload(songId: String) {
        viewModelScope.launch {
            downloadWorkManager.resumeDownload(songId)
        }
    }

    fun cancelDownload(songId: String) {
        viewModelScope.launch {
            downloadWorkManager.cancelDownload(songId)
        }
    }

    fun deleteDownloadedSong(songId: String) {
        viewModelScope.launch {
            try {
                downloadWorkManager.deleteCompletedDownload(
                    songId
                )
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = exception.message
                            ?: "Couldn't delete downloaded song."
                    )
                }
            }
        }
    }
}
