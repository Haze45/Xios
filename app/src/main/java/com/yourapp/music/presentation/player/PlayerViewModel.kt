package com.yourapp.music.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.core.player.MusicPlayerServiceConnection
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.download.DownloadSongUseCase
import com.yourapp.music.domain.usecase.download.IsSongDownloadedUseCase
import com.yourapp.music.domain.usecase.download.IsSongDownloadingUseCase
import com.yourapp.music.domain.usecase.favorite.AddFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.IsFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: MusicPlayerServiceConnection,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val favoriteSyncManager: FavoriteSyncManager,
    private val downloadSongUseCase: DownloadSongUseCase,
    private val isSongDownloadedUseCase: IsSongDownloadedUseCase,
    private val isSongDownloadingUseCase: IsSongDownloadingUseCase
) : ViewModel() {

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    /** Re-subscribes to isFavorite() whenever the currently playing song changes. */
    private val favoriteFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            val uid = userId
            if (song == null || uid == null) flowOf(false) else isFavoriteUseCase(song.id, uid)
        }

    private val downloadedFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            if (song == null) flowOf(false) else isSongDownloadedUseCase(song.id)
        }

    /**
     * In-flight state comes from the singleton repository (not local to
     * this ViewModel), so it stays correct if the person leaves the Player
     * screen mid-download and comes back to a fresh ViewModel instance.
     */
    private val downloadingFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            if (song == null) flowOf(false) else isSongDownloadingUseCase(song.id)
        }

    /** One-shot message for the UI (e.g. a Toast), cleared via consumeDownloadMessage(). */
    private val downloadMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlayerUiState> = combine(
        connection.playerState,
        favoriteFlow,
        downloadedFlow,
        downloadingFlow,
        downloadMessage
    ) { state, isFav, isDownloaded, isDownloading, message ->
        PlayerUiState(
            currentSong = state.currentSong,
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isShuffleEnabled = state.isShuffleEnabled,
            isFavorite = isFav,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    init {
        connection.connect()
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) = connection.playSong(song, queue)
    fun togglePlayPause() = connection.togglePlayPause()
    fun skipToNext() = connection.skipToNext()
    fun skipToPrevious() = connection.skipToPrevious()
    fun toggleShuffle() = connection.toggleShuffle()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun toggleFavorite() {
        val song = uiState.value.currentSong ?: return
        val uid = userId ?: return
        val currentlyFavorite = uiState.value.isFavorite
        viewModelScope.launch {
            if (currentlyFavorite) removeFavoriteUseCase(song.id, uid) else addFavoriteUseCase(song.id, uid)
            favoriteSyncManager.enqueueSync()
        }
    }

    /** Downloads the current song into Downloads/MusicApp. No-op if already downloaded or in progress. */
    fun downloadCurrentSong() {
        val state = uiState.value
        val song = state.currentSong ?: return
        if (state.isDownloaded || state.isDownloading) return

        viewModelScope.launch {
            downloadMessage.value = when (val result = downloadSongUseCase(song)) {
                is Resource.Success -> "Downloaded to Downloads/MusicApp"
                is Resource.Error -> result.message
                is Resource.Loading -> null
            }
        }
    }

    /** Called by the UI once it has shown the message, so it isn't shown again on recomposition. */
    fun consumeDownloadMessage() {
        downloadMessage.value = null
    }

    // Intentionally no onCleared() override here. `connection` is an
    // app-wide @Singleton shared by every screen that can control
    // playback (Home, Library, Player, Search, PlaylistDetail) — it
    // must outlive any single screen's PlayerViewModel. Releasing it
    // when just one screen's ViewModel is cleared (e.g. backing out of
    // the Player screen) would kill playback control for the entire
    // app, not just that screen. The connection is torn down once, at
    // the process/application level, in MusicPlayerServiceConnection
    // itself if needed — not per-ViewModel.
}
