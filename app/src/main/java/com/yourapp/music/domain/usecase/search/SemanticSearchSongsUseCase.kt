package com.yourapp.music.domain.usecase.search

import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SearchRepository
import javax.inject.Inject

class SemanticSearchSongsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        matchCount: Int = 20
    ): List<Song> {
        return searchRepository.semanticSearch(
            query = query,
            matchCount = matchCount
        )
    }
}
