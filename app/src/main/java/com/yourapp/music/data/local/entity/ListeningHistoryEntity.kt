package com.yourapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_history",
    indices = [
        Index(value = ["userId", "playedAt"]),
        Index(value = ["songId"])
    ]
)
data class ListeningHistoryEntity(
    @PrimaryKey
    val sessionId: String,

    val userId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String,

    val playedAt: Long,
    val durationMs: Long,
    val listenedMs: Long,
    val lastPositionMs: Long,

    val eventType: String = "PLAYING",

    // Used later for offline synchronization.
    val isSynced: Boolean = false
)
