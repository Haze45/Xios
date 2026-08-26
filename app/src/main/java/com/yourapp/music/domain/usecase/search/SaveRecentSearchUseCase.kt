package com.yourapp.music.domain.usecase.search

import com.yourapp.music.domain.repository.SearchRepository
import javax.inject.Inject

class SaveRecentSearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(query: String) {
        if (query.isBlank()) return
        searchRepository.saveRecentSearch(query.trim())
    }
}
