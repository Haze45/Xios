package com.yourapp.music.core.download

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yourapp.music.data.local.dao.DownloadDao
import com.yourapp.music.data.local.dao.DownloadTaskDao
import com.yourapp.music.data.local.entity.DownloadTaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri

@Singleton
class DownloadWorkManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadTaskDao: DownloadTaskDao,
    private val downloadDao: DownloadDao
) {
    companion object {
        const val DOWNLOAD_TAG = "music_background_download"

        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }

    private val workManager =
        WorkManager.getInstance(context)

    suspend fun enqueueDownload(songId: String) {
        val existing = downloadTaskDao.getTask(songId)

        if (existing == null) {
            downloadTaskDao.upsertTask(
                DownloadTaskEntity(
                    songId = songId,
                    temporaryFilePath = "",
                    status = STATUS_QUEUED
                )
            )
        } else {
            downloadTaskDao.updateStatus(
                songId,
                STATUS_QUEUED
            )
        }

        enqueueWork(songId, ExistingWorkPolicy.KEEP)
    }

    suspend fun pauseDownload(songId: String) {
        val task = downloadTaskDao.getTask(songId) ?: return

        if (
            task.status == STATUS_COMPLETED ||
            task.status == STATUS_CANCELLED
        ) {
            return
        }

        // Set the state before stopping the worker.
        downloadTaskDao.updateStatus(
            songId,
            STATUS_PAUSED
        )

        stopAndWait(songId)
    }

    suspend fun resumeDownload(songId: String) {
        val task = downloadTaskDao.getTask(songId) ?: return

        if (
            task.status != STATUS_PAUSED &&
            task.status != STATUS_QUEUED &&
            task.status != STATUS_FAILED
        ) {
            return
        }

        downloadTaskDao.updateStatus(
            songId,
            STATUS_QUEUED
        )

        enqueueWork(
            songId,
            ExistingWorkPolicy.REPLACE
        )
    }

    suspend fun cancelDownload(songId: String) {
        val task = downloadTaskDao.getTask(songId) ?: return

        if (task.status == STATUS_COMPLETED) {
            return
        }

        // Tell the worker not to overwrite the cancellation state.
        downloadTaskDao.updateStatus(
            songId,
            STATUS_CANCELLED
        )

        // Wait until the worker has actually stopped writing.
        stopAndWait(songId)

        val latestTask = downloadTaskDao.getTask(songId)

        // If completion won the race, preserve the completed download.
        if (latestTask?.status == STATUS_COMPLETED) {
            return
        }

        withContext(Dispatchers.IO) {
            latestTask?.temporaryFilePath
                ?.takeIf { it.isNotBlank() }
                ?.let { path ->
                    File(path).delete()
                }

            // Remove an unregistered final file left by an interrupted
            // completion, but never remove a registered download.
            val alreadyDownloaded =
                downloadDao.isDownloaded(songId).first()

            if (!alreadyDownloaded) {
                val directory = File(
                    context.noBackupFilesDir,
                    "encrypted_downloads"
                )

                val finalFile = File(
                    directory,
                    "${sha256(songId)}.xenc"
                )

                if (finalFile.exists()) {
                    finalFile.delete()
                }
            }

            downloadTaskDao.deleteTask(songId)
        }
    }

    private suspend fun stopAndWait(songId: String) {
        val workName = uniqueWorkName(songId)

        withContext(Dispatchers.IO) {
            // Wait for WorkManager to accept the cancellation request.
            workManager.cancelUniqueWork(workName).result.get()

            // Cancellation is asynchronous. Do not touch the file
            // until every associated worker has reached a terminal state.
            while (true) {
                val workInfos = workManager
                    .getWorkInfosForUniqueWork(workName)
                    .get()

                if (
                    workInfos.isEmpty() ||
                    workInfos.all { it.state.isFinished }
                ) {
                    break
                }

                delay(150L.milliseconds)
            }
        }
    }

    private fun enqueueWork(
        songId: String,
        policy: ExistingWorkPolicy
    ) {
        val request =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(
                            DownloadWorker.KEY_SONG_ID,
                            songId
                        )
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag(DOWNLOAD_TAG)
                .addTag(uniqueWorkName(songId))
                .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(songId),
            policy,
            request
        )
    }

    fun uniqueWorkName(songId: String): String =
        "song_download_$songId"

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }

    suspend fun deleteCompletedDownload(songId: String) {
        val download =
            downloadDao.getDownload(songId) ?: return

        withContext(Dispatchers.IO) {
            val localPath =
                download.localUri.toUri().path
                    ?: throw IOException("Invalid encrypted download path.")

            val encryptedFile = File(localPath)

            if (encryptedFile.exists() && !encryptedFile.delete()) {
                throw IOException("Couldn't delete the encrypted song file.")
            }

            // Remove the Room record only after file deletion succeeds.
            downloadDao.deleteDownload(songId)
        }
    }
}
