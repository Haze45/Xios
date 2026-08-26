package com.yourapp.music.domain.repository

import com.yourapp.music.domain.model.RecentSearch
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    /** Room-cached, most recent first. */
    fun getRecentSearches(): Flow<List<RecentSearch>>
    suspend fun saveRecentSearch(query: String)
    suspend fun clearRecentSearches()
}
