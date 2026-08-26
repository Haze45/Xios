package com.yourapp.music.domain.usecase.search

import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    operator fun invoke(): Flow<List<RecentSearch>> = searchRepository.getRecentSearches()
}
