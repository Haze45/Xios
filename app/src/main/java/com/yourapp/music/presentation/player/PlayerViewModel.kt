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
import com.yourapp.music.domain.usecase.download.ObserveDownloadProgressUseCase
import com.yourapp.music.domain.usecase.favorite.AddFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.IsFavoriteUseCase
import com.yourapp.music.domain.usecase.favorite.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    private val isSongDownloadingUseCase: IsSongDownloadingUseCase,
    private val observeDownloadProgressUseCase: ObserveDownloadProgressUseCase
) : ViewModel() {

    private val userId: String?
        get() = getCurrentUserUseCase()?.uid

    private val favoriteFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            val uid = userId

            if (song == null || uid == null) {
                flowOf(false)
            } else {
                isFavoriteUseCase(song.id, uid)
            }
        }

    private val downloadedFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            if (song == null) {
                flowOf(false)
            } else {
                isSongDownloadedUseCase(song.id)
            }
        }

    private val downloadingFlow = connection.playerState
        .map { it.currentSong }
        .flatMapLatest { song ->
            if (song == null) {
                flowOf(false)
            } else {
                isSongDownloadingUseCase(song.id)
            }
        }

    // Observe actual download progress for the currently playing song.
    private val downloadProgressFlow =
        connection.playerState
            .map { it.currentSong }
            .flatMapLatest { song ->
                if (song == null) {
                    flowOf(null)
                } else {
                    observeDownloadProgressUseCase().map { downloads ->
                        downloads.firstOrNull {
                            it.song.id == song.id
                        }?.fraction
                    }
                }
            }

    private val downloadMessage =
        MutableStateFlow<String?>(null)

    private val downloadUiFlow = combine(
        downloadProgressFlow,
        downloadMessage
    ) { progress, message ->
        progress to message
    }

    val uiState = combine(
        connection.playerState,
        favoriteFlow,
        downloadedFlow,
        downloadingFlow,
        downloadUiFlow
    ) { state, isFav, isDownloaded, isDownloading, downloadUi ->

        PlayerUiState(
            currentSong = state.currentSong,
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isShuffleEnabled = state.isShuffleEnabled,
            isFavorite = isFav,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgressFraction = downloadUi.first,
            downloadMessage = downloadUi.second
        )

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlayerUiState()
    )

    init {
        connection.connect()
    }

    fun playSong(
        song: Song,
        queue: List<Song> = listOf(song)
    ) =
        connection.playSong(song, queue)

    fun togglePlayPause() = connection.togglePlayPause()

    fun skipToNext() = connection.skipToNext()

    fun skipToPrevious() = connection.skipToPrevious()

    fun toggleShuffle() = connection.toggleShuffle()

    fun seekTo(positionMs: Long) =
        connection.seekTo(positionMs)

    fun toggleFavorite() {
        val song = uiState.value.currentSong ?: return
        val uid = userId ?: return
        val currentlyFavorite = uiState.value.isFavorite

        viewModelScope.launch {
            if (currentlyFavorite) {
                removeFavoriteUseCase(song.id, uid)
            } else {
                addFavoriteUseCase(song.id, uid)
            }

            favoriteSyncManager.enqueueSync()
        }
    }

    fun downloadCurrentSong() {
        val state = uiState.value
        val song = state.currentSong ?: return

        if (state.isDownloaded || state.isDownloading) return

        viewModelScope.launch {
            downloadMessage.value =
                when (val result =
                    downloadSongUseCase(song)) {
                    is Resource.Success ->
                        "Download started in background"

                    is Resource.Error ->
                        result.message

                    is Resource.Loading -> null
                }
        }
    }

    fun consumeDownloadMessage() {
        downloadMessage.value = null
    }
}
