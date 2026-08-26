package com.yourapp.music.domain.usecase.favorite

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.FavoriteRepository
import javax.inject.Inject

class SyncFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(userId: String): Resource<Unit> = favoriteRepository.syncFavorites(userId)
}
