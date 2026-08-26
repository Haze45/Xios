package com.yourapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "",
    val albumArtUrl: String,
    val audioUrl: String,
    val durationMs: Long
)
