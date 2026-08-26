package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import javax.inject.Inject

class GetTopDailyPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(): Resource<List<Playlist>> = playlistRepository.getTopDailyPlaylists()
}
