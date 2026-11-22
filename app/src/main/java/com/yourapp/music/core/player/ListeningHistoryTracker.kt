package com.yourapp.music.core.player

import android.os.SystemClock
import androidx.media3.common.Player
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.domain.model.ListeningHistoryEventType
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.ListeningHistoryRepository
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningHistoryTracker @Inject constructor(
    private val historyRepository: ListeningHistoryRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val favoriteSyncManager: FavoriteSyncManager
) {
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val writeMutex = Mutex()

    private var currentSessionId: String? = null
    private var accumulatedPlayedMs = 0L
    private var segmentStartedAtMs: Long? = null
    private var lastPersistedAtMs = 0L
    private var lastPositionMs = 0L
    private var durationMs = 0L

    fun onSongTransition(
        song: Song,
        transitionReason: Int,
        isPlaying: Boolean,
        durationMs: Long
    ) {
        val previousPosition = lastPositionMs
        val previousDuration = this.durationMs

        val completed =
            transitionReason ==
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    (
                            previousDuration > 0L &&
                                    previousPosition >= previousDuration * 0.8
                            )

        finishCurrentSession(
            eventType = if (completed) {
                ListeningHistoryEventType.COMPLETED
            } else {
                ListeningHistoryEventType.SKIPPED
            },
            positionMs = previousPosition
        )

        startSession(
            song = song,
            isPlaying = isPlaying,
            durationMs = durationMs
        )
    }

    fun onPlaybackChanged(
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        lastPositionMs = positionMs.coerceAtLeast(0L)
        this.durationMs = durationMs.coerceAtLeast(0L)

        if (currentSessionId == null) return

        if (isPlaying) {
            if (segmentStartedAtMs == null) {
                segmentStartedAtMs =
                    SystemClock.elapsedRealtime()
            }
        } else {
            accumulateCurrentSegment()
            persistProgress(syncAfterWrite = true)
        }
    }

    fun onProgress(
        positionMs: Long,
        durationMs: Long
    ) {
        lastPositionMs = positionMs.coerceAtLeast(0L)
        this.durationMs = durationMs.coerceAtLeast(0L)

        if (currentSessionId == null) return

        val now = SystemClock.elapsedRealtime()

        if (now - lastPersistedAtMs >= 15_000L) {
            persistProgress(syncAfterWrite = false)
        }
    }

    fun onPlaybackEnded() {
        finishCurrentSession(
            eventType = ListeningHistoryEventType.COMPLETED,
            positionMs = durationMs
        )
    }

    fun onServiceStopped(
        positionMs: Long,
        durationMs: Long
    ) {
        finishCurrentSession(
            eventType = ListeningHistoryEventType.INTERRUPTED,
            positionMs = positionMs
        )

        this.durationMs = durationMs.coerceAtLeast(0L)
    }

    private fun startSession(
        song: Song,
        isPlaying: Boolean,
        durationMs: Long
    ) {
        val userId = getCurrentUserUseCase()?.uid ?: return

        val sessionId = UUID.randomUUID().toString()
        val playedAt = System.currentTimeMillis()

        currentSessionId = sessionId
        accumulatedPlayedMs = 0L
        lastPositionMs = 0L
        this.durationMs = durationMs.coerceAtLeast(0L)
        lastPersistedAtMs = SystemClock.elapsedRealtime()

        segmentStartedAtMs =
            if (isPlaying) {
                SystemClock.elapsedRealtime()
            } else {
                null
            }

        scope.launch {
            writeMutex.withLock {
                historyRepository.startSession(
                    sessionId = sessionId,
                    song = song,
                    userId = userId,
                    playedAt = playedAt
                )
            }
        }
    }

    private fun finishCurrentSession(
        eventType: ListeningHistoryEventType,
        positionMs: Long
    ) {
        val sessionId = currentSessionId ?: return

        lastPositionMs = positionMs.coerceAtLeast(0L)
        accumulateCurrentSegment()

        val finalListenedMs = accumulatedPlayedMs
        val finalPositionMs = lastPositionMs

        currentSessionId = null
        segmentStartedAtMs = null
        accumulatedPlayedMs = 0L

        scope.launch {
            writeMutex.withLock {
                historyRepository.finishSession(
                    sessionId = sessionId,
                    listenedMs = finalListenedMs,
                    lastPositionMs = finalPositionMs,
                    eventType = eventType.name
                )

                // Queue cloud sync only after the Room update succeeds.
                favoriteSyncManager.enqueueSync()
            }
        }
    }

    private fun accumulateCurrentSegment() {
        val segmentStart = segmentStartedAtMs ?: return

        val elapsed =
            SystemClock.elapsedRealtime() - segmentStart

        accumulatedPlayedMs += elapsed.coerceAtLeast(0L)
        segmentStartedAtMs = null
    }

    private fun persistProgress(
        syncAfterWrite: Boolean
    ) {
        val sessionId = currentSessionId ?: return
        val now = SystemClock.elapsedRealtime()

        val currentSegmentMs =
            segmentStartedAtMs?.let {
                (now - it).coerceAtLeast(0L)
            } ?: 0L

        val totalListenedMs =
            accumulatedPlayedMs + currentSegmentMs

        val positionSnapshot = lastPositionMs
        lastPersistedAtMs = now

        scope.launch {
            writeMutex.withLock {
                historyRepository.updateProgress(
                    sessionId = sessionId,
                    listenedMs = totalListenedMs,
                    lastPositionMs = positionSnapshot
                )

                if (syncAfterWrite) {
                    favoriteSyncManager.enqueueSync()
                }
            }
        }
    }
}
