package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import javax.inject.Inject

class GetPlaylistByIdUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(id: String): Playlist? = playlistRepository.getPlaylistById(id)
}
