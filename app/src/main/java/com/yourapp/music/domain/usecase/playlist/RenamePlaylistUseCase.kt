package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.PlaylistRepository
import javax.inject.Inject

class RenamePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, newTitle: String): Resource<Unit> {
        if (newTitle.isBlank()) return Resource.Error("Playlist name cannot be empty")
        return playlistRepository.renamePlaylist(playlistId, newTitle.trim())
    }
}
