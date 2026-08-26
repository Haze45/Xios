package com.yourapp.music.domain.usecase.search

import com.yourapp.music.domain.repository.SearchRepository
import javax.inject.Inject

class ClearRecentSearchesUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke() = searchRepository.clearRecentSearches()
}
