package com.yourapp.music.data.local.entity

import androidx.room.Entity

/**
 * isSynced=false means this row was changed locally and still needs
 * to be pushed to Supabase by the background SyncWorker.
 */
@Entity(tableName = "favorites", primaryKeys = ["songId", "userId"])
data class FavoriteEntity(
    val songId: String,
    val userId: String,
    val addedAt: Long,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false // soft-delete flag so removal can also sync
)
