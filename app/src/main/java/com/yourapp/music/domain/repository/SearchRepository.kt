package com.yourapp.music.domain.repository

import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    /** Room-cached, most recent first. */
    fun getRecentSearches(): Flow<List<RecentSearch>>

    suspend fun saveRecentSearch(query: String)

    suspend fun clearRecentSearches()

    /** Searches the remote semantic index using the signed-in Firebase user. */
    suspend fun semanticSearch(
        query: String,
        matchCount: Int = 20
    ): List<Song>
}
