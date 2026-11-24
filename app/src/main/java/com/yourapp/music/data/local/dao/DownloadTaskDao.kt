package com.yourapp.music.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yourapp.music.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Upsert
    suspend fun upsertTask(task: DownloadTaskEntity)

    @Query(
        "SELECT * FROM download_tasks WHERE songId = :songId LIMIT 1"
    )
    suspend fun getTask(songId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks ORDER BY updatedAt DESC")
    fun observeTasks(): Flow<List<DownloadTaskEntity>>

    @Query(
        "SELECT * FROM download_tasks WHERE status = :status ORDER BY updatedAt DESC"
    )
    fun observeTasksByStatus(
        status: String
    ): Flow<List<DownloadTaskEntity>>

    @Query(
        """
        UPDATE download_tasks
        SET downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes,
            updatedAt = :updatedAt
        WHERE songId = :songId
        """
    )
    suspend fun updateProgress(
        songId: String,
        downloadedBytes: Long,
        totalBytes: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE download_tasks
        SET status = :status,
            updatedAt = :updatedAt
        WHERE songId = :songId
        """
    )
    suspend fun updateStatus(
        songId: String,
        status: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM download_tasks WHERE songId = :songId")
    suspend fun deleteTask(songId: String)

    @Query("DELETE FROM download_tasks")
    suspend fun deleteAllTasks()
}
