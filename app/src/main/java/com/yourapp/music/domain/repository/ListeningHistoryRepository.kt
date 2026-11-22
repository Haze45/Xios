package com.yourapp.music.domain.repository

import com.yourapp.music.domain.model.ListeningHistoryEntry
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface ListeningHistoryRepository {

    suspend fun startSession(
        sessionId: String,
        song: Song,
        userId: String,
        playedAt: Long
    )

    suspend fun updateProgress(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long
    )

    suspend fun finishSession(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long,
        eventType: String
    )

    fun observeHistory(
        userId: String,
        limit: Int = 50
    ): Flow<List<ListeningHistoryEntry>>

    suspend fun syncPendingHistory(
        userId: String,
        firebaseIdToken: String
    ): Int
}
