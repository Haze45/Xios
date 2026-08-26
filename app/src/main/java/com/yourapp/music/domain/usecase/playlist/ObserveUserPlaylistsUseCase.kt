package com.yourapp.music.domain.usecase.playlist

import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Live, Room-backed view of a user's playlists — reflects create/add-song/
 * delete instantly everywhere it's collected, without needing a manual
 * refresh. Call GetUserPlaylistsUseCase once first to make sure the Room
 * cache is actually hydrated (this Flow only ever reads local data).
 */
class ObserveUserPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(userId: String): Flow<List<Playlist>> = playlistRepository.observeUserPlaylists(userId)
}
