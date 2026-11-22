package com.yourapp.music.domain.repository

import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.model.SemanticSongMatch
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    /** Room-cached, most recent first. */
    fun getRecentSearches(): Flow<List<RecentSearch>>

    suspend fun saveRecentSearch(query: String)

    suspend fun clearRecentSearches()

    /** Returns semantic matches with their similarity scores. */
    suspend fun semanticSearch(
        query: String,
        matchCount: Int = 20
    ): List<SemanticSongMatch>
}
