package com.yourapp.music.data.local.entity

import androidx.room.Entity

@Entity(tableName = "followed_artists", primaryKeys = ["id", "userId"])
data class ArtistEntity(
    val id: String,
    val userId: String,
    val name: String,
    val avatarUrl: String
)
