package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistByIdUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(id: String): Flow<Playlist?> = playlistRepository.observePlaylistById(id)
}
