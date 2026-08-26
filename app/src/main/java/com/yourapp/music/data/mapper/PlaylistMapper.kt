package com.yourapp.music.data.mapper

import com.yourapp.music.data.local.entity.PlaylistEntity
import com.yourapp.music.data.remote.supabase.dto.PlaylistDto
import com.yourapp.music.domain.model.Playlist

fun PlaylistDto.toEntity() = PlaylistEntity(
    id = id,
    title = title,
    ownerId = owner_id,
    ownerName = owner_name,
    coverUrl = cover_url,
    songCount = song_count,
    songIds = song_ids
)

fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    title = title,
    ownerId = ownerId,
    ownerName = ownerName,
    coverUrl = coverUrl,
    songCount = songCount,
    songIds = songIds
)

fun PlaylistDto.toDomain() = Playlist(
    id = id,
    title = title,
    ownerId = owner_id,
    ownerName = owner_name,
    coverUrl = cover_url,
    songCount = song_count,
    description = description,
    songIds = song_ids
)
