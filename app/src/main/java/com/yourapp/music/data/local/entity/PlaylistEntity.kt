package com.yourapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val ownerId: String,
    val ownerName: String,
    val coverUrl: String,
    val songCount: Int,
    val songIds: List<String> = emptyList()
)
