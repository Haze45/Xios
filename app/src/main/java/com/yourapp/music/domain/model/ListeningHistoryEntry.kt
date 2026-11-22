package com.yourapp.music.domain.model

data class ListeningHistoryEntry(
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
    val eventType: ListeningHistoryEventType
)