package com.yourapp.music.data.repository

import com.yourapp.music.data.local.dao.ListeningHistoryDao
import com.yourapp.music.data.local.entity.ListeningHistoryEntity
import com.yourapp.music.data.remote.supabase.ListeningHistoryApi
import com.yourapp.music.domain.model.ListeningHistoryEntry
import com.yourapp.music.domain.model.ListeningHistoryEventType
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.ListeningHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningHistoryRepositoryImpl @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val listeningHistoryApi: ListeningHistoryApi
) : ListeningHistoryRepository {

    override suspend fun startSession(
        sessionId: String,
        song: Song,
        userId: String,
        playedAt: Long
    ) {
        listeningHistoryDao.insertSession(
            ListeningHistoryEntity(
                sessionId = sessionId,
                userId = userId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                albumArtUrl = song.albumArtUrl,
                playedAt = playedAt,
                durationMs = song.durationMs,
                listenedMs = 0L,
                lastPositionMs = 0L,
                eventType = ListeningHistoryEventType.PLAYING.name,
                isSynced = false
            )
        )
    }

    override suspend fun updateProgress(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long
    ) {
        listeningHistoryDao.updateProgress(
            sessionId,
            listenedMs,
            lastPositionMs
        )
    }

    override suspend fun finishSession(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long,
        eventType: String
    ) {
        listeningHistoryDao.finishSession(
            sessionId,
            listenedMs,
            lastPositionMs,
            eventType
        )
    }

    override fun observeHistory(
        userId: String,
        limit: Int
    ): Flow<List<ListeningHistoryEntry>> =
        listeningHistoryDao.observeHistory(userId, limit)
            .map { entries ->
                entries.map { entry ->
                    ListeningHistoryEntry(
                        sessionId = entry.sessionId,
                        userId = entry.userId,
                        songId = entry.songId,
                        title = entry.title,
                        artist = entry.artist,
                        albumArtUrl = entry.albumArtUrl,
                        playedAt = entry.playedAt,
                        durationMs = entry.durationMs,
                        listenedMs = entry.listenedMs,
                        lastPositionMs = entry.lastPositionMs,
                        eventType = runCatching {
                            ListeningHistoryEventType.valueOf(
                                entry.eventType
                            )
                        }.getOrDefault(
                            ListeningHistoryEventType.PLAYING
                        )
                    )
                }
            }

    override suspend fun syncPendingHistory(
        userId: String,
        firebaseIdToken: String
    ): Int {
        val pending =
            listeningHistoryDao.getUnsyncedHistory(
                userId = userId,
                limit = 100
            )

        if (pending.isEmpty()) return 0

        // Mark records synced only after the server confirms the batch.
        listeningHistoryApi.syncHistory(
            events = pending,
            firebaseIdToken = firebaseIdToken
        )

        pending.forEach { event ->
            listeningHistoryDao.markSynced(
                sessionId = event.sessionId,
                userId = userId,
                listenedMs = event.listenedMs,
                lastPositionMs = event.lastPositionMs,
                eventType = event.eventType
            )
        }

        return pending.size
    }
}
