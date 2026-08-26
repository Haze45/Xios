package com.yourapp.music.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.core.player.MusicPlayerServiceConnection
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.AddFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.IsFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val favoriteSyncManager: FavoriteSyncManager
) : ViewModel() {

    private val userId: String? get() = getCurrentUserUseCase()?.uid

    /** Re-subscribes to isFavorite() whenever the currently playing song changes. */
    private val favoriteFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            val uid = userId
            if (song == null || uid == null) flowOf(false) else isFavoriteUseCase(song.id, uid)
        }

    val uiState: StateFlow<PlayerUiState> = combine(connection.playerState, favoriteFlow) { state, isFav ->
        PlayerUiState(
            currentSong = state.currentSong,
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isShuffleEnabled = state.isShuffleEnabled,
            isFavorite = isFav
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
