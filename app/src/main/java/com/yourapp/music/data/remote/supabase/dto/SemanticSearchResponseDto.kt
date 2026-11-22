package com.yourapp.music.data.remote.supabase.dto

import com.yourapp.music.domain.model.SemanticSongMatch
import com.yourapp.music.domain.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SemanticSearchResponseDto(
    val success: Boolean,
    val error: String? = null,
    val results: List<SemanticSongDto> = emptyList()
)

@Serializable
data class SemanticSongDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val genre: String? = null,

    @SerialName("album_art_url")
    val albumArtUrl: String,

    @SerialName("audio_url")
    val audioUrl: String,

    @SerialName("duration_ms")
    val durationMs: Long,

    @SerialName("is_new_release")
    val isNewRelease: Boolean = false,

    @SerialName("is_trending")
    val isTrending: Boolean = false,

    val similarity: Double? = null
) {
    fun toDomainMatch(): SemanticSongMatch {
        val song = Song(
            id = id,
            title = title,
            artist = artist,
            album = album.orEmpty(),
            genre = genre.orEmpty(),
            albumArtUrl = albumArtUrl,
            audioUrl = audioUrl,
            durationMs = durationMs,
            isNewRelease = isNewRelease,
            isTrending = isTrending
        )

        return SemanticSongMatch(
            song = song,
            similarity = similarity
        )
    }
}
