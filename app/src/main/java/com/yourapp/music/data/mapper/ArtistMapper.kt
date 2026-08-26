package com.yourapp.music.data.mapper

import com.yourapp.music.data.local.entity.ArtistEntity
import com.yourapp.music.data.remote.supabase.dto.ArtistDto
import com.yourapp.music.domain.model.Artist

fun ArtistDto.toEntity(userId: String) = ArtistEntity(
    id = id,
    userId = userId,
    name = name,
    avatarUrl = avatar_url
)

fun ArtistEntity.toDomain() = Artist(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    isFollowing = true // only followed artists are ever cached locally
)
