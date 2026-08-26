package com.yourapp.music.data.mapper

import com.yourapp.music.data.local.entity.FavoriteEntity
import com.yourapp.music.data.remote.supabase.dto.FavoriteDto

fun FavoriteDto.toEntity(isSynced: Boolean = true) = FavoriteEntity(
    songId = song_id,
    userId = user_id,
    addedAt = added_at,
    isSynced = isSynced,
    isDeleted = false
)

fun FavoriteEntity.toDto() = FavoriteDto(
    song_id = songId,
    user_id = userId,
    added_at = addedAt
)
