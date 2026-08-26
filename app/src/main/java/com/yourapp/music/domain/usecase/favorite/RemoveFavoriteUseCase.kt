package com.yourapp.music.domain.usecase.favorite

import com.yourapp.music.domain.repository.FavoriteRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(songId: String, userId: String) =
        favoriteRepository.removeFavorite(songId, userId)
}
