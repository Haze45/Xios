package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(title: String, ownerId: String): Resource<Playlist> {
        if (title.isBlank()) return Resource.Error("Playlist name cannot be empty")
        return playlistRepository.createPlaylist(title.trim(), ownerId)
    }
}
