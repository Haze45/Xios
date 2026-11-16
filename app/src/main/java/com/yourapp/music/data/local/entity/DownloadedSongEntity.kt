package com.yourapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which songs have been downloaded to the device and where the
 * actual audio file lives (a MediaStore content:// Uri on Android 10+,
 * or a plain file:// path on older versions).
 */
@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val localUri: String,
    val downloadedAt: Long
)
