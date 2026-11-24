package com.yourapp.music.core.download

import android.content.Context
import android.net.Uri
import android.system.Os
import android.util.Base64
import com.yourapp.music.data.local.dao.DownloadTaskDao
import com.yourapp.music.data.local.entity.DownloadTaskEntity
import com.yourapp.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ENCRYPTED_SCHEME = "xios-encrypted"

@Singleton
class SongDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadTaskDao: DownloadTaskDao
) {

    suspend fun download(
        song: Song,
        onProgress: suspend (
            downloadedBytes: Long,
            totalBytes: Long?
        ) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {

        val directory = File(
            context.noBackupFilesDir,
            "encrypted_downloads"
        )

        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException(
                "Couldn't create encrypted downloads folder."
            )
        }

        val fileName = createFileName(song.id)
        val destination = File(directory, fileName)
        val temporaryFile =
            File(directory, "$fileName.part")

        val sourceUrlHash = sha256(song.audioUrl)

        var task = downloadTaskDao.getTask(song.id)

        // A changed URL must never be appended to an old partial file.
        if (
            task != null &&
            task.sourceUrlHash != null &&
            task.sourceUrlHash != sourceUrlHash
        ) {
            temporaryFile.delete()
            task = null
        }

        val expectedSalt = task?.encryptionSalt?.let {
            Base64.decode(it, Base64.NO_WRAP)
        }

        val writer = ResumableSongEncryption.openOrCreate(
            destination = temporaryFile,
            expectedSalt = expectedSalt
        )

        expectedSalt?.fill(0)

        val salt = writer.salt.copyOf()
        val resumeOffset = writer.resumeOffsetBytes

        val connection = openConnection(
            song.audioUrl,
            resumeOffset
        )

        try {
            connection.connect()

            val responseCode = connection.responseCode

            val etag = connection.getHeaderField("ETag")
            val lastModified =
                connection.getHeaderField("Last-Modified")

            val responseTotal = getResponseTotalLength(
                connection,
                responseCode
            )

            val sourceChanged =
                task != null &&
                        (
                                task.sourceUrlHash != sourceUrlHash ||
                                        (
                                                task.sourceETag != null &&
                                                        etag != null &&
                                                        task.sourceETag != etag
                                                ) ||
                                        (
                                                task.sourceLastModified != null &&
                                                        lastModified != null &&
                                                        task.sourceLastModified != lastModified
                                                ) ||
                                        (
                                                task.sourceLength != null &&
                                                        responseTotal != null &&
                                                        task.sourceLength != responseTotal
                                                )
                                )

            val rangeAccepted =
                resumeOffset == 0L ||
                        (
                                responseCode == HttpURLConnection.HTTP_PARTIAL &&
                                        getRangeStart(
                                            connection.getHeaderField(
                                                "Content-Range"
                                            )
                                        ) == resumeOffset
                                )

            if (sourceChanged || !rangeAccepted) {
                writer.close()
                connection.disconnect()

                temporaryFile.delete()

                downloadTaskDao.upsertTask(
                    DownloadTaskEntity(
                        songId = song.id,
                        temporaryFilePath = temporaryFile.absolutePath,
                        sourceUrlHash = sourceUrlHash,
                        status = DownloadWorkManager.STATUS_RUNNING
                    )
                )

                return@withContext downloadFromStart(
                    song = song,
                    temporaryFile = temporaryFile,
                    destination = destination,
                    sourceUrlHash = sourceUrlHash,
                    onProgress = onProgress
                )
            }

            if (
                responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                throw IOException(
                    "Download failed: HTTP $responseCode"
                )
            }

            if (
                resumeOffset > 0L &&
                responseTotal != null &&
                resumeOffset > responseTotal
            ) {
                throw IOException(
                    "Invalid resume offset."
                )
            }

            // Persist enough metadata to reopen this partial file.
            downloadTaskDao.upsertTask(
                DownloadTaskEntity(
                    songId = song.id,
                    temporaryFilePath = temporaryFile.absolutePath,
                    downloadedBytes = resumeOffset,
                    totalBytes = responseTotal,
                    status = DownloadWorkManager.STATUS_RUNNING,
                    sourceETag = etag,
                    sourceLastModified = lastModified,
                    sourceLength = responseTotal,
                    sourceUrlHash = sourceUrlHash,
                    encryptionSalt = Base64.encodeToString(
                        salt,
                        Base64.NO_WRAP
                    ),
                    chunkCount = writer.chunkCount
                )
            )

            val finalLength =
                connection.inputStream.use { input ->
                    writer.append(input) { committedBytes ->
                        onProgress(
                            committedBytes,
                            responseTotal
                        )
                    }
                }

            writer.close()

            if (
                responseTotal != null &&
                finalLength != responseTotal
            ) {
                throw IOException(
                    "Downloaded length doesn't match source length."
                )
            }

            if (
                !temporaryFile.exists() ||
                temporaryFile.length() == 0L
            ) {
                throw IOException(
                    "Encrypted download file is empty."
                )
            }

            // The completed encrypted file replaces the previous copy.
            Os.rename(
                temporaryFile.absolutePath,
                destination.absolutePath
            )

            downloadTaskDao.updateProgress(
                songId = song.id,
                downloadedBytes = finalLength,
                totalBytes = responseTotal
            )

            Uri.Builder()
                .scheme(ENCRYPTED_SCHEME)
                .path(destination.absolutePath)
                .build()
                .toString()

        } catch (exception: Exception) {
            writer.close()
            throw exception

        } finally {
            connection.disconnect()
            salt.fill(0)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadFromStart(
        song: Song,
        temporaryFile: File,
        destination: File,
        sourceUrlHash: String,
        onProgress: suspend (
            downloadedBytes: Long,
            totalBytes: Long?
        ) -> Unit
    ): String {
        temporaryFile.delete()

        val writer = ResumableSongEncryption.openOrCreate(
            destination = temporaryFile
        )

        val salt = writer.salt.copyOf()
        val connection = openConnection(song.audioUrl, 0L)

        try {
            connection.connect()

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException(
                    "Fresh download failed: HTTP $responseCode"
                )
            }

            val responseTotal =
                connection.contentLengthLong.takeIf { it >= 0L }

            val etag = connection.getHeaderField("ETag")
            val lastModified =
                connection.getHeaderField("Last-Modified")

            downloadTaskDao.upsertTask(
                DownloadTaskEntity(
                    songId = song.id,
                    temporaryFilePath = temporaryFile.absolutePath,
                    downloadedBytes = 0L,
                    totalBytes = responseTotal,
                    status = DownloadWorkManager.STATUS_RUNNING,
                    sourceETag = etag,
                    sourceLastModified = lastModified,
                    sourceLength = responseTotal,
                    sourceUrlHash = sourceUrlHash,
                    encryptionSalt = Base64.encodeToString(
                        salt,
                        Base64.NO_WRAP
                    ),
                    chunkCount = 0
                )
            )

            val finalLength =
                connection.inputStream.use { input ->
                    writer.append(input) { committedBytes ->
                        onProgress(
                            committedBytes,
                            responseTotal
                        )
                    }
                }

            writer.close()

            if (
                responseTotal != null &&
                finalLength != responseTotal
            ) {
                throw IOException(
                    "Downloaded length doesn't match source length."
                )
            }

            Os.rename(
                temporaryFile.absolutePath,
                destination.absolutePath
            )

            downloadTaskDao.updateProgress(
                songId = song.id,
                downloadedBytes = finalLength,
                totalBytes = responseTotal
            )

            return Uri.Builder()
                .scheme(ENCRYPTED_SCHEME)
                .path(destination.absolutePath)
                .build()
                .toString()

        } catch (exception: Exception) {
            writer.close()
            throw exception

        } finally {
            connection.disconnect()
            salt.fill(0)
        }
    }

    private fun openConnection(
        audioUrl: String,
        resumeOffset: Long
    ): HttpURLConnection {
        val connection =
            URL(audioUrl).openConnection() as HttpURLConnection

        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true

        if (resumeOffset > 0L) {
            connection.setRequestProperty(
                "Range",
                "bytes=$resumeOffset-"
            )
        }

        return connection
    }

    private fun getResponseTotalLength(
        connection: HttpURLConnection,
        responseCode: Int
    ): Long? {
        if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
            val range =
                connection.getHeaderField("Content-Range")
                    ?: return null

            return range.substringAfterLast("/")
                .takeIf { it != "*" }
                ?.toLongOrNull()
        }

        return connection.contentLengthLong.takeIf {
            it >= 0L
        }
    }

    private fun getRangeStart(
        contentRange: String?
    ): Long? {
        if (contentRange == null) return null

        return contentRange
            .substringAfter("bytes ", "")
            .substringBefore("-")
            .toLongOrNull()
    }

    private fun createFileName(songId: String): String {
        return sha256(songId) + ".xenc"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}
