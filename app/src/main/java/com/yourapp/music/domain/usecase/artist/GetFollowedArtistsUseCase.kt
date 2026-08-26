package com.yourapp.music.domain.usecase.artist

import com.yourapp.music.domain.model.Artist
import com.yourapp.music.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowedArtistsUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    operator fun invoke(userId: String): Flow<List<Artist>> = artistRepository.getFollowedArtists(userId)
}
