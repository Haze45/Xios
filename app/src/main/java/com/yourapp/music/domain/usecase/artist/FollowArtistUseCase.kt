package com.yourapp.music.domain.usecase.artist

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.ArtistRepository
import javax.inject.Inject

class FollowArtistUseCase @Inject constructor(
    private val artistRepository: ArtistRepository
) {
    suspend operator fun invoke(artistId: String, userId: String): Resource<Unit> =
        artistRepository.followArtist(artistId, userId)
}
