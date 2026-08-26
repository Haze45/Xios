package com.yourapp.music.domain.usecase.favorite

import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(userId: String): Flow<List<Song>> = favoriteRepository.getFavoriteSongs(userId)
}
