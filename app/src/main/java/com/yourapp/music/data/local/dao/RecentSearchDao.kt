package com.yourapp.music.data.local.dao

import androidx.room.*
import com.yourapp.music.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
