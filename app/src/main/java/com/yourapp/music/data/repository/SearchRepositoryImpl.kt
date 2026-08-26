package com.yourapp.music.data.repository

import com.yourapp.music.data.local.dao.RecentSearchDao
import com.yourapp.music.data.local.entity.RecentSearchEntity
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao
) : SearchRepository {

    override fun getRecentSearches(): Flow<List<RecentSearch>> =
        recentSearchDao.getRecentSearches().map { list -> list.map { it.toDomain() } }

    override suspend fun saveRecentSearch(query: String) {
        recentSearchDao.upsertSearch(RecentSearchEntity(query = query, searchedAt = System.currentTimeMillis()))
    }

    override suspend fun clearRecentSearches() = recentSearchDao.clearAll()
}
