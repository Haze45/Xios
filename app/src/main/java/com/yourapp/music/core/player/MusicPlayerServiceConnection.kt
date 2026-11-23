package com.yourapp.music.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yourapp.music.data.local.datastore.PlaybackStateStore
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Singleton
class MusicPlayerServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackStateStore: PlaybackStateStore,
    private val songRepository: SongRepository
) {
    private var controller: MediaController? = null

    private var isConnecting = false

    private var pendingPlayRequest: Pair<Song, List<Song>>? =
        null

    private var restoredButNotLoaded: RestoredPlayback? =
        null

    private data class RestoredPlayback(
        val song: Song,
        val queue: List<Song>,
        val positionMs: Long
    )

    private var originalQueueOrder: List<Song>? = null

    private val connectionScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Smooth playback position updates on the main thread.
    private val positionScope =
        CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var positionUpdateJob: Job? = null

    private val _playerState =
        MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> =
        _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update {
                it.copy(isPlaying = isPlaying)
            }

            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()

                val positionMs = controller?.currentPosition

                if (positionMs != null) {
                    _playerState.update {
                        it.copy(
                            positionMs = positionMs,
                            durationMs = controller?.duration
                                ?.coerceAtLeast(0)
                                ?: it.durationMs
                        )
                    }

                    connectionScope.launch {
                        playbackStateStore.savePosition(
                            positionMs
                        )
                    }
                }
            }
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            val newSong = _playerState.value.queue
                .firstOrNull { it.id == mediaItem?.mediaId }

            if (newSong != null) {
                _playerState.update {
                    it.copy(
                        currentSong = newSong,
                        currentIndex = controller?.currentMediaItemIndex
                            ?: it.currentIndex
                    )
                }

                connectionScope.launch {
                    playbackStateStore.saveLastPlayed(
                        newSong.id,
                        _playerState.value.queue.map { it.id },
                        0L
                    )
                }
            }
        }

        override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
            _playerState.update {
                it.copy(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(
                        0
                    )
                )
            }
        }
    }

    /**
     * Updates playback position every 50 milliseconds.
     * This makes the Compose slider progress smoothly.
     */
    private fun startPositionUpdates() {
        if (positionUpdateJob?.isActive == true) return

        positionUpdateJob = positionScope.launch {
            while (isActive) {
                val activeController = controller ?: break

                if (!activeController.isPlaying) break

                _playerState.update {
                    it.copy(
                        positionMs = activeController.currentPosition,
                        durationMs = activeController.duration.coerceAtLeast(
                            0
                        )
                    )
                }

                delay(50L.milliseconds)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun connect() {
        if (controller != null || isConnecting) return

        isConnecting = true

        val sessionToken = SessionToken(
            context,
            ComponentName(
                context,
                PlaybackService::class.java
            )
        )

        val controllerFuture =
            MediaController.Builder(context, sessionToken)
                .buildAsync()

        controllerFuture.addListener({

            controller = controllerFuture.get()
            controller?.addListener(playerListener)

            isConnecting = false

            val pending = pendingPlayRequest

            if (pending != null) {
                pendingPlayRequest = null
                playSong(pending.first, pending.second)

            } else if (_playerState.value.currentSong == null) {
                restoreLastSession()
            }

        }, MoreExecutors.directExecutor())
    }

    /**
     * Restores previous playback without automatically starting it.
     */
    private fun restoreLastSession() {
        connectionScope.launch {

            val last = playbackStateStore.getLastPlayed()
                ?: return@launch

            val song =
                songRepository.getSongById(last.songId)
                    ?: return@launch

            val queue = last.queueSongIds
                .mapNotNull {
                    songRepository.getSongById(it)
                }
                .ifEmpty {
                    listOf(song)
                }

            restoredButNotLoaded = RestoredPlayback(
                song,
                queue,
                last.positionMs
            )

            _playerState.update {
                it.copy(
                    currentSong = song,
                    queue = queue,
                    currentIndex = queue.indexOfFirst { q ->
                        q.id == song.id
                    }.coerceAtLeast(0),
                    positionMs = last.positionMs,
                    durationMs = song.durationMs,
                    isPlaying = false
                )
            }
        }
    }

    fun playSong(
        song: Song,
        queue: List<Song> = listOf(song)
    ) {
        val activeController = controller

        if (activeController == null) {
            pendingPlayRequest = song to queue
            connect()
            return
        }

        restoredButNotLoaded = null

        playInternal(
            activeController,
            song,
            queue,
            startPositionMs = 0L
        )
    }

    private fun playInternal(
        activeController: MediaController,
        song: Song,
        queue: List<Song>,
        startPositionMs: Long,
        shouldPlay: Boolean = true
    ) {
        originalQueueOrder = null

        val mediaItems = queue.map {
            MediaItem.Builder()
                .setUri(it.audioUrl)
                .setMediaId(it.id)
                .build()
        }

        val startIndex = queue.indexOfFirst {
            it.id == song.id
        }.coerceAtLeast(0)

        activeController.setMediaItems(
            mediaItems,
            startIndex,
            startPositionMs
        )

        activeController.prepare()

        if (shouldPlay) {
            activeController.play()
        } else {
            activeController.pause()
        }

        _playerState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                currentIndex = startIndex,
                isPlaying = shouldPlay,
                isShuffleEnabled = false
            )
        }

        connectionScope.launch {
            playbackStateStore.saveLastPlayed(
                song.id,
                queue.map { it.id },
                startPositionMs
            )
        }
    }

    fun togglePlayPause() {
        if (loadRestoredSessionIfNeeded()) return

        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipToNext() {
        val activeController = controller ?: return

        // Restore the queue if it hasn't been loaded into ExoPlayer.
        loadRestoredSessionIfNeeded(shouldPlay = true)

        if (activeController.mediaItemCount == 0) return

        activeController.seekToNextMediaItem()
        activeController.play()
    }

    fun skipToPrevious() {
        loadRestoredSessionIfNeeded()
        controller?.seekToPreviousMediaItem()
    }

    private fun loadRestoredSessionIfNeeded(
        shouldPlay: Boolean = true
    ): Boolean {
        val restored = restoredButNotLoaded
        val activeController = controller

        if (restored == null || activeController == null) {
            return false
        }

        restoredButNotLoaded = null

        playInternal(
            activeController,
            restored.song,
            restored.queue,
            startPositionMs = restored.positionMs,
            shouldPlay = shouldPlay
        )

        return true
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun playQueueItem(index: Int) {
        // Validate against the queue shown by the app.
        if (index !in _playerState.value.queue.indices) return

        val activeController = controller ?: run {
            connect()
            return
        }
        // A restored queue may be visible in the UI but not yet loaded
        // into ExoPlayer. Load it before seeking to the tapped item.
        loadRestoredSessionIfNeeded()

        // Make sure the requested item now exists in the player timeline.
        if (index !in 0 until activeController.mediaItemCount) return

        activeController.seekToDefaultPosition(index)
        activeController.play()

        // Update the UI immediately; the media transition listener will
        // also reconcile the state with ExoPlayer.
        _playerState.update { state ->
            state.copy(
                currentSong = state.queue[index],
                currentIndex = index,
                isPlaying = true,
                positionMs = 0L
            )
        }
    }

    fun removeFromQueue(index: Int) {
        val activeController = controller ?: return
        val queue = _playerState.value.queue

        if (index !in queue.indices) return

        val removedSongId = queue[index].id

        activeController.removeMediaItem(index)

        val newQueue = queue.toMutableList().apply {
            removeAt(index)
        }

        val newIndex =
            activeController.currentMediaItemIndex

        originalQueueOrder =
            originalQueueOrder?.filterNot {
                it.id == removedSongId
            }

        _playerState.update {
            it.copy(
                queue = newQueue,
                currentIndex = newIndex,
                currentSong = if (newQueue.isEmpty()) {
                    null
                } else {
                    newQueue.getOrNull(newIndex)
                        ?: it.currentSong
                }
            )
        }
    }

    fun toggleShuffle() {
        loadRestoredSessionIfNeeded()

        val activeController = controller ?: return
        val state = _playerState.value
        val currentIndex =
            state.currentIndex.coerceAtLeast(0)

        if (!state.isShuffleEnabled) {

            originalQueueOrder = state.queue

            val played = state.queue.subList(
                0,
                (currentIndex + 1).coerceAtMost(state.queue.size)
            )

            val upcoming = state.queue.subList(
                (currentIndex + 1).coerceAtMost(state.queue.size),
                state.queue.size
            )

            val shuffledUpcoming = upcoming.shuffled()
            val newQueue = played + shuffledUpcoming

            replaceUpcomingMediaItems(
                activeController,
                currentIndex,
                shuffledUpcoming
            )

            _playerState.update {
                it.copy(
                    queue = newQueue,
                    isShuffleEnabled = true
                )
            }

        } else {

            val original = originalQueueOrder ?: state.queue
            val currentSongId = state.currentSong?.id

            val restoredIndex = original.indexOfFirst {
                it.id == currentSongId
            }.coerceAtLeast(0)

            val upcomingOriginal = original.subList(
                (restoredIndex + 1).coerceAtMost(original.size),
                original.size
            )

            replaceUpcomingMediaItems(
                activeController,
                restoredIndex,
                upcomingOriginal
            )

            originalQueueOrder = null

            _playerState.update {
                it.copy(
                    queue = original,
                    currentIndex = restoredIndex,
                    isShuffleEnabled = false
                )
            }
        }
    }

    private fun replaceUpcomingMediaItems(
        controller: MediaController,
        fromIndex: Int,
        newUpcoming: List<Song>
    ) {
        val removeFrom = fromIndex + 1

        if (removeFrom < controller.mediaItemCount) {
            controller.removeMediaItems(
                removeFrom,
                controller.mediaItemCount
            )
        }

        if (newUpcoming.isNotEmpty()) {

            val newMediaItems = newUpcoming.map {
                MediaItem.Builder()
                    .setUri(it.audioUrl)
                    .setMediaId(it.id)
                    .build()
            }

            controller.addMediaItems(
                removeFrom,
                newMediaItems
            )
        }
    }

    fun release() {
        stopPositionUpdates()

        controller?.removeListener(playerListener)
        controller?.release()

        controller = null
        isConnecting = false
        pendingPlayRequest = null
    }
}
