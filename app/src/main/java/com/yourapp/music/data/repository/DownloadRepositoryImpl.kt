package com.yourapp.music.data.repository

import com.yourapp.music.core.download.DownloadWorkManager
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.dao.DownloadTaskDao
import com.yourapp.music.domain.model.DownloadProgress
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.DownloadRepository
import com.yourapp.music.domain.repository.SongRepository
import com.yourapp.music.core.util.Resource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadWorkManager: DownloadWorkManager,
    private val downloadDao: DownloadDao,
    private val downloadTaskDao: DownloadTaskDao,
    private val songRepository: SongRepository
) : DownloadRepository {

    override fun getDownloadedSongs(): Flow<List<Song>> =
        combine(
            downloadDao.getAllDownloads(),
            songRepository.getAllSongs()
        ) { downloads, allSongs ->

            val songsById = allSongs.associateBy { it.id }

            downloads.mapNotNull { download ->
                songsById[download.songId]?.copy(
                    audioUrl = download.localUri
                )
            }
        }

    override fun isDownloaded(songId: String): Flow<Boolean> =
        downloadDao.isDownloaded(songId)

    override fun isDownloading(songId: String): Flow<Boolean> =
        downloadTaskDao.observeTasks()
            .let { tasksFlow ->
                tasksFlow.combine(
                    kotlinx.coroutines.flow.flowOf(Unit)
                ) { tasks, _ ->
                    tasks.any { task ->
                        task.songId == songId &&
                                (
                                        task.status ==
                                                DownloadWorkManager.STATUS_QUEUED ||
                                                task.status ==
                                                DownloadWorkManager.STATUS_RUNNING
                                        )
                    }
                }
            }

    override fun observeDownloadProgress(): Flow<List<DownloadProgress>> =
        combine(
            downloadTaskDao.observeTasks(),
            songRepository.getAllSongs()
        ) { tasks, songs ->

            val songsById = songs.associateBy { it.id }

            tasks.mapNotNull { task ->

                // Completed downloads are displayed in the
                // downloaded songs section instead.
                if (
                    task.status ==
                    DownloadWorkManager.STATUS_COMPLETED
                ) {
                    return@mapNotNull null
                }

                if (
                    task.status !in setOf(
                        DownloadWorkManager.STATUS_QUEUED,
                        DownloadWorkManager.STATUS_RUNNING,
                        DownloadWorkManager.STATUS_PAUSED,
                        DownloadWorkManager.STATUS_FAILED
                    )
                ) {
                    return@mapNotNull null
                }

                val song = songsById[task.songId]
                    ?: return@mapNotNull null

                DownloadProgress(
                    song = song,
                    downloadedBytes = task.downloadedBytes,
                    totalBytes = task.totalBytes,
                    status = task.status
                )
            }
        }

    override suspend fun downloadSong(
        song: Song
    ): Resource<Unit> {
        return try {
            downloadWorkManager.enqueueDownload(song.id)
            Resource.Success(Unit)
        } catch (exception: Exception) {
            Resource.Error(
                exception.message
                    ?: "Couldn't schedule download",
                exception
            )
        }
    }
}
