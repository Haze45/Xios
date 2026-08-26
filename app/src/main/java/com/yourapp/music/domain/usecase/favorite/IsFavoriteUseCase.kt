package com.yourapp.music.domain.usecase.favorite

import com.yourapp.music.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(songId: String, userId: String): Flow<Boolean> =
        favoriteRepository.isFavorite(songId, userId)
}
