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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single connection point between the UI (PlayerViewModel) and the
 * PlaybackService. Wraps a MediaController and exposes playback state
 * as a Flow so any screen (mini-player, full player) can observe it.
 *
 * This is an app-wide @Singleton — every screen that can trigger
 * playback shares the same instance and the same underlying
 * MediaController connection. It should never be released by an
 * individual screen's ViewModel.onCleared(); it lives for as long as
 * the app process does.
 */
@UnstableApi
@Singleton
class MusicPlayerServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackStateStore: PlaybackStateStore,
    private val songRepository: SongRepository
) {
    private var controller: MediaController? = null

    // Guards against building a second MediaController while the first
    // connect() call is still completing asynchronously (e.g. two
    // different screens both calling connect() from their init{} within
    // the same frame).
    private var isConnecting = false

    // If playSong() is called before the controller has finished
    // connecting, we remember the request here and run it as soon as
    // the connection completes, instead of silently dropping it.
    private var pendingPlayRequest: Pair<Song, List<Song>>? = null

    // Set once, right after a fresh process start, if we found a
    // previously-playing song saved from last time. It only describes
    // what to SHOW (MiniPlayer/PlayerScreen) — the real ExoPlayer queue
    // hasn't actually been loaded with it yet. The first time the person
    // actually taps play, we consume this and cue it into the player for
    // real, seeking to where they left off.
    private var restoredButNotLoaded: RestoredPlayback? = null

    private data class RestoredPlayback(val song: Song, val queue: List<Song>, val positionMs: Long)

    // Background scope for persistence I/O — this class isn't a ViewModel
    // and outlives any single screen, so it manages its own small scope
    // for the lifetime of the app process.
    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }

            // Whenever playback pauses — whether the person tapped pause,
            // playback reached the end, or the system paused it for any
            // other reason — save roughly where they were, so reopening
            // the app later resumes near this point instead of from zero.
            if (!isPlaying) {
                val positionMs = controller?.currentPosition ?: return
                connectionScope.launch { playbackStateStore.savePosition(positionMs) }
            }
        }

        // Fires whenever Media3 moves to a different item in the queue —
        // whether from seekToNextMediaItem()/seekToPreviousMediaItem()
        // (our skip buttons), auto-advance at the end of a track, a
        // notification/lock-screen skip, or picking a song directly from
        // the queue sheet. Without this, currentSong would stay stuck on
        // whatever song started playback, even though the audio itself
        // had already moved on.
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val newSong = _playerState.value.queue.firstOrNull { it.id == mediaItem?.mediaId }
            if (newSong != null) {
                _playerState.update {
                    it.copy(currentSong = newSong, currentIndex = controller?.currentMediaItemIndex ?: it.currentIndex)
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

        override fun onEvents(player: Player, events: Player.Events) {
            _playerState.update {
                it.copy(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0)
                )
            }
        }
    }

    fun connect() {
        if (controller != null || isConnecting) return
        isConnecting = true

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.addListener(playerListener)
            isConnecting = false

            // Run whatever playback request came in while we were still connecting.
            val pending = pendingPlayRequest
            if (pending != null) {
                pendingPlayRequest = null
                playSong(pending.first, pending.second)
            } else if (_playerState.value.currentSong == null) {
                // Nothing already queued in this process — see if there's a
                // song from a previous session worth showing in the MiniPlayer.
                restoreLastSession()
            }
        }, MoreExecutors.directExecutor())
    }

    /** Populates currentSong/queue/position from the last session, WITHOUT starting playback. */
    private fun restoreLastSession() {
        connectionScope.launch {
            val last = playbackStateStore.getLastPlayed() ?: return@launch
            val song = songRepository.getSongById(last.songId) ?: return@launch
            val queue = last.queueSongIds.mapNotNull { songRepository.getSongById(it) }
                .ifEmpty { listOf(song) }

            restoredButNotLoaded = RestoredPlayback(song, queue, last.positionMs)
            _playerState.update {
                it.copy(
                    currentSong = song,
                    queue = queue,
                    currentIndex = queue.indexOfFirst { q -> q.id == song.id }.coerceAtLeast(0),
                    positionMs = last.positionMs,
                    durationMs = song.durationMs,
                    isPlaying = false
                )
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val activeController = controller
        if (activeController == null) {
            // Not connected yet — remember this request and make sure a
            // connection attempt is in flight; connect() will replay it
            // once ready.
            pendingPlayRequest = song to queue
            connect()
            return
        }
        restoredButNotLoaded = null // a real, explicit play always overrides any restored placeholder
        playInternal(activeController, song, queue, startPositionMs = 0L)
    }

    private fun playInternal(activeController: MediaController, song: Song, queue: List<Song>, startPositionMs: Long) {
        val mediaItems = queue.map {
            MediaItem.Builder()
                .setUri(it.audioUrl)
                .setMediaId(it.id)
                .build()
        }
        val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        activeController.setMediaItems(mediaItems, startIndex, startPositionMs)
        activeController.prepare()
        activeController.play()
        _playerState.update { it.copy(currentSong = song, queue = queue, currentIndex = startIndex) }

        connectionScope.launch {
            playbackStateStore.saveLastPlayed(song.id, queue.map { it.id }, startPositionMs)
        }
    }

    fun togglePlayPause() {
        if (loadRestoredSessionIfNeeded()) return // just cued + started playing from where we left off
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipToNext() {
        loadRestoredSessionIfNeeded()
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        loadRestoredSessionIfNeeded()
        controller?.seekToPreviousMediaItem()
    }

    /**
     * If we're still showing a restored-but-not-loaded MiniPlayer state
     * (app was just reopened, nothing tapped yet), cues it into the
     * player for real now. Returns true if it did so, so callers can
     * follow up with the action the person actually asked for (e.g.
     * skip to next, right after loading the restored song in).
     */
    private fun loadRestoredSessionIfNeeded(): Boolean {
        val restored = restoredButNotLoaded
        val activeController = controller
        if (restored == null || activeController == null) return false
        restoredButNotLoaded = null
        playInternal(activeController, restored.song, restored.queue, startPositionMs = restored.positionMs)
        return true
    }

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    /** Jumps playback straight to a specific song in the current queue (tap-to-play from the queue sheet). */
    fun playQueueItem(index: Int) {
        val activeController = controller ?: return
        if (index !in _playerState.value.queue.indices) return
        restoredButNotLoaded = null
        activeController.seekTo(index, 0L)
        activeController.play()
        _playerState.update {
            it.copy(currentSong = it.queue[index], currentIndex = index)
        }
    }

    /** Removes a song from the queue (swipe-to-remove from the queue sheet). */
    fun removeFromQueue(index: Int) {
        val activeController = controller ?: return
        val queue = _playerState.value.queue
        if (index !in queue.indices) return

        activeController.removeMediaItem(index)

        val newQueue = queue.toMutableList().apply { removeAt(index) }
        val newIndex = activeController.currentMediaItemIndex
        _playerState.update {
            it.copy(
                queue = newQueue,
                currentIndex = newIndex,
                currentSong = if (newQueue.isEmpty()) null else newQueue.getOrNull(newIndex) ?: it.currentSong
            )
        }
    }

    fun toggleShuffle() {
        val enabled = !(_playerState.value.isShuffleEnabled)
        controller?.shuffleModeEnabled = enabled
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }

    /**
     * Tears down the MediaController connection entirely. NOT meant to be
     * called from any screen-scoped ViewModel's onCleared() — this is a
     * process-wide teardown only (e.g. hook it into Application-level
     * cleanup if you ever need one). Calling this from a single screen's
     * ViewModel would kill playback control for the whole app.
     */
    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        isConnecting = false
        pendingPlayRequest = null
    }
}
