package com.yourapp.music.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourapp.music.data.local.entity.ListeningHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ListeningHistoryEntity)

    @Query(
        """
        UPDATE listening_history
        SET listenedMs = :listenedMs,
            lastPositionMs = :lastPositionMs,
            isSynced = 0
        WHERE sessionId = :sessionId
        """
    )
    suspend fun updateProgress(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long
    )

    @Query(
        """
        UPDATE listening_history
        SET listenedMs = :listenedMs,
            lastPositionMs = :lastPositionMs,
            eventType = :eventType,
            isSynced = 0
        WHERE sessionId = :sessionId
        """
    )
    suspend fun finishSession(
        sessionId: String,
        listenedMs: Long,
        lastPositionMs: Long,
        eventType: String
    )

    @Query(
        """
        SELECT * FROM listening_history
        WHERE userId = :userId
        ORDER BY playedAt DESC
        LIMIT :limit
        """
    )
    fun observeHistory(
        userId: String,
        limit: Int = 50
    ): Flow<List<ListeningHistoryEntity>>

    @Query(
        """
        SELECT * FROM listening_history
        WHERE userId = :userId
        ORDER BY playedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentHistory(
        userId: String,
        limit: Int = 50
    ): List<ListeningHistoryEntity>

    @Query(
        """
        SELECT * FROM listening_history
        WHERE userId = :userId
        AND isSynced = 0
        ORDER BY playedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getUnsyncedHistory(
        userId: String,
        limit: Int = 100
    ): List<ListeningHistoryEntity>

    @Query(
        """
    UPDATE listening_history
    SET isSynced = 1
    WHERE sessionId = :sessionId
    AND userId = :userId
    AND listenedMs = :listenedMs
    AND lastPositionMs = :lastPositionMs
    AND eventType = :eventType
    """
    )
    suspend fun markSynced(
        sessionId: String,
        userId: String,
        listenedMs: Long,
        lastPositionMs: Long,
        eventType: String
    )

    @Query(
        """
        DELETE FROM listening_history
        WHERE userId = :userId
        AND sessionId NOT IN (
            SELECT sessionId FROM listening_history
            WHERE userId = :userId
            ORDER BY playedAt DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun trimHistory(
        userId: String,
        keepCount: Int = 500
    )
}
