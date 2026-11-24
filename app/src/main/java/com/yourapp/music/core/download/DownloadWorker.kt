package com.yourapp.music.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.dao.DownloadTaskDao
import com.yourapp.music.data.local.entity.DownloadedSongEntity
import com.yourapp.music.domain.repository.SongRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val songRepository: SongRepository,
    private val songDownloader: SongDownloader,
    private val downloadDao: DownloadDao,
    private val downloadTaskDao: DownloadTaskDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SONG_ID = "download_song_id"
        const val KEY_PROGRESS = "download_progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"

        private const val CHANNEL_ID = "music_downloads"
        private const val NOTIFICATION_ID_BASE = 4000
        private const val TAG = "DownloadWorker"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID)
            ?: return Result.failure()

        return try {
            val song = songRepository.getSongById(songId)
                ?: run {
                    downloadTaskDao.updateStatus(
                        songId,
                        DownloadWorkManager.STATUS_FAILED
                    )
                    return Result.failure()
                }

            // Don't start a task that was paused or cancelled.
            val task = downloadTaskDao.getTask(songId)

            if (
                task?.status == DownloadWorkManager.STATUS_PAUSED ||
                task?.status == DownloadWorkManager.STATUS_CANCELLED
            ) {
                return Result.failure()
            }

            downloadTaskDao.updateStatus(
                songId,
                DownloadWorkManager.STATUS_RUNNING
            )

            createNotificationChannel()
            setForeground(createForegroundInfo(song.title))

            var lastDownloaded = 0L
            var lastTotal: Long? = null

            val encryptedUri =
                songDownloader.download(song) { downloadedBytes, totalBytes ->

                    lastDownloaded = downloadedBytes
                    lastTotal = totalBytes

                    val percent = totalBytes
                        ?.takeIf { it > 0L }
                        ?.let {
                            ((downloadedBytes * 100L) / it)
                                .coerceIn(0L, 100L)
                                .toInt()
                        } ?: 0

                    // No runBlocking: execute suspend operations normally.
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to percent,
                            KEY_DOWNLOADED_BYTES to downloadedBytes,
                            KEY_TOTAL_BYTES to (totalBytes
                                ?: -1L)
                        )
                    )

                    downloadTaskDao.updateProgress(
                        songId = songId,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    )

                    notificationManager.notify(
                        notificationId(songId),
                        createNotification(
                            song.title,
                            percent,
                            totalBytes != null
                        )
                    )
                }

            // Check again before publishing the completed download.
            val latestTask = downloadTaskDao.getTask(songId)

            if (
                latestTask?.status == DownloadWorkManager.STATUS_PAUSED ||
                latestTask?.status == DownloadWorkManager.STATUS_CANCELLED
            ) {
                return Result.failure()
            }

            downloadDao.upsertDownload(
                DownloadedSongEntity(
                    songId = song.id,
                    localUri = encryptedUri,
                    downloadedAt = System.currentTimeMillis()
                )
            )

            downloadTaskDao.updateProgress(
                songId = songId,
                downloadedBytes = lastDownloaded,
                totalBytes = lastTotal
            )

            downloadTaskDao.updateStatus(
                songId,
                DownloadWorkManager.STATUS_COMPLETED
            )

            notificationManager.notify(
                notificationId(songId),
                createNotification(
                    song.title,
                    100,
                    false,
                    completed = true
                )
            )

            Log.i(TAG, "Download completed: songId=$songId")

            Result.success()

        } catch (exception: CancellationException) {

            Log.w(
                TAG,
                "Worker cancelled: songId=$songId",
                exception
            )

            // A cancelled coroutine cannot safely perform ordinary
            // suspending Room operations. Use NonCancellable here.
            withContext(NonCancellable) {
                val currentTask =
                    downloadTaskDao.getTask(songId)

                when (currentTask?.status) {
                    DownloadWorkManager.STATUS_PAUSED,
                    DownloadWorkManager.STATUS_CANCELLED -> {
                        // Preserve intentional user actions.
                    }

                    else -> {
                        // Unexpected interruption.
                        downloadTaskDao.updateStatus(
                            songId,
                            DownloadWorkManager.STATUS_FAILED
                        )
                    }
                }
            }

            throw exception

        } catch (exception: IOException) {

            Log.e(
                TAG,
                "Download I/O or network failure: songId=$songId",
                exception
            )

            val currentTask =
                downloadTaskDao.getTask(songId)

            if (
                currentTask?.status != DownloadWorkManager.STATUS_PAUSED &&
                currentTask?.status != DownloadWorkManager.STATUS_CANCELLED
            ) {
                downloadTaskDao.updateStatus(
                    songId,
                    DownloadWorkManager.STATUS_FAILED
                )
            }

            // Keep the partial encrypted file for a manual retry.
            Result.failure()

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unexpected download failure: songId=$songId",
                exception
            )

            val currentTask =
                downloadTaskDao.getTask(songId)

            if (
                currentTask?.status != DownloadWorkManager.STATUS_PAUSED &&
                currentTask?.status != DownloadWorkManager.STATUS_CANCELLED
            ) {
                downloadTaskDao.updateStatus(
                    songId,
                    DownloadWorkManager.STATUS_FAILED
                )
            }

            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows music download progress"
        }

        notificationManager.createNotificationChannel(
            channel
        )
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification = createNotification(
            title = title,
            progress = 0,
            indeterminate = true
        )

        val id = notificationId(
            inputData.getString(KEY_SONG_ID).orEmpty()
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun createNotification(
        title: String,
        progress: Int,
        indeterminate: Boolean,
        completed: Boolean = false
    ): android.app.Notification {
        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (completed) "Download complete"
                else "Downloading music"
            )
            .setContentText(
                if (completed) "$title is ready offline"
                else title
            )
            .setOnlyAlertOnce(true)
            .setOngoing(!completed)
            .setProgress(100, progress, indeterminate)
            .build()
    }

    private fun notificationId(songId: String): Int =
        NOTIFICATION_ID_BASE +
                (songId.hashCode() and 0x3FFFFFFF)
}
