package com.yourapp.music.presentation.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.core.player.MusicPlayerServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Thin wrapper around the app-wide [MusicPlayerServiceConnection] for the
 * queue bottom sheet. Doesn't own any playback state itself — just maps
 * the shared PlayerState down to what the queue UI needs, and forwards
 * user actions (tap-to-play, swipe-to-remove) back to the connection.
 */
@UnstableApi
@HiltViewModel
class QueueViewModel @Inject constructor(
    private val connection: MusicPlayerServiceConnection
) : ViewModel() {

    val uiState: StateFlow<QueueUiState> = connection.playerState
        .map { state ->
            QueueUiState(
                queue = state.queue,
                currentIndex = state.currentIndex,
                isPlaying = state.isPlaying
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QueueUiState())

    fun playAt(index: Int) = connection.playQueueItem(index)
    fun removeAt(index: Int) = connection.removeFromQueue(index)
    fun togglePlayPause() = connection.togglePlayPause()

    // No onCleared() override — same reasoning as PlayerViewModel: `connection`
    // is an app-wide singleton and must outlive this sheet-scoped ViewModel.
}
