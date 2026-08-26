package com.yourapp.music.data.mapper

import com.yourapp.music.data.local.entity.SongEntity
import com.yourapp.music.data.remote.supabase.dto.SongDto
import com.yourapp.music.domain.model.Song

fun SongDto.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    albumArtUrl = album_art_url,
    audioUrl = audio_url,
    durationMs = duration_ms
)

fun SongEntity.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    durationMs = durationMs
)

fun SongDto.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    albumArtUrl = album_art_url,
    audioUrl = audio_url,
    durationMs = duration_ms,
    isNewRelease = is_new_release,
    isTrending = is_trending
)
