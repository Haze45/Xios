package com.yourapp.music.data.local.entity

import androidx.room.Entity

@Entity(tableName = "recent_searches", primaryKeys = ["query"])
data class RecentSearchEntity(
    val query: String,
    val searchedAt: Long
)
