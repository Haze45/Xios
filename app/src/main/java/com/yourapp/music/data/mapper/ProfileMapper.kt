package com.yourapp.music.data.mapper

import com.yourapp.music.data.remote.supabase.dto.ProfileDto
import com.yourapp.music.domain.model.UserStats

fun ProfileDto.toDomain() = UserStats(
    followingCount = following_count,
    followersCount = followers_count,
    playlistCount = playlist_count
)
