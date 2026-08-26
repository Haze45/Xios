package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String): Resource<Unit> =
        playlistRepository.deletePlaylist(playlistId)
}
