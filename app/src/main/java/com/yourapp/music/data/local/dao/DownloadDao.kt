package com.yourapp.music.data.local.dao

import androidx.room.*
import com.yourapp.music.data.local.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun isDownloaded(songId: String): Flow<Boolean>

    @Upsert
    suspend fun upsertDownload(download: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun deleteDownload(songId: String)
}
