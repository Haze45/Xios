package com.yourapp.music.core.download

import android.content.Context
import android.net.Uri
import android.system.Os
import com.yourapp.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ENCRYPTED_SCHEME = "xios-encrypted"

private const val STALE_PART_FILE_AGE_MS =
    24L * 60L * 60L * 1000L

@Singleton
class SongDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: SongEncryptionManager
) {

    private val activeTemporaryFiles =
        ConcurrentHashMap.newKeySet<String>()

    suspend fun download(song: Song): String =
        withContext(Dispatchers.IO) {

            val downloadDirectory = File(
                context.noBackupFilesDir,
                "encrypted_downloads"
            )

            if (
                !downloadDirectory.exists() &&
                !downloadDirectory.mkdirs()
            ) {
                throw IOException(
                    "Couldn't create the encrypted downloads folder."
                )
            }

            // Remove abandoned temporary files from previous downloads.
            cleanupAbandonedTemporaryFiles(downloadDirectory)

            val fileName = createFileName(song.id)

            val destination = File(
                downloadDirectory,
                fileName
            )

            // Every download gets its own temporary file.
            val temporaryFile = File(
                downloadDirectory,
                "$fileName.${UUID.randomUUID()}.part"
            )

            if (!temporaryFile.createNewFile()) {
                throw IOException(
                    "Couldn't create the temporary download file."
                )
            }

            activeTemporaryFiles.add(
                temporaryFile.absolutePath
            )

            val connection =
                URL(song.audioUrl).openConnection()
                        as HttpURLConnection

            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = true

            try {
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw IOException(
                        "Download failed: HTTP ${connection.responseCode}"
                    )
                }

                connection.inputStream.use { input ->

                    // Stream, encrypt and write one chunk at a time.
                    encryptionManager.encrypt(
                        input = input,
                        destination = temporaryFile
                    )
                }

                if (!temporaryFile.exists() ||
                    temporaryFile.length() == 0L
                ) {
                    throw IOException(
                        "Encrypted download file is empty."
                    )
                }

                /*
                 * Atomically replaces the previous download.
                 *
                 * Both files are in the same directory, so the rename
                 * happens on the same filesystem. The old destination
                 * remains intact if the rename operation fails.
                 */
                Os.rename(
                    temporaryFile.absolutePath,
                    destination.absolutePath
                )

                Uri.Builder()
                    .scheme(ENCRYPTED_SCHEME)
                    .path(destination.absolutePath)
                    .build()
                    .toString()

            } catch (exception: Exception) {

                // Keep the previous successful download untouched.
                temporaryFile.delete()

                throw exception

            } finally {

                connection.disconnect()

                activeTemporaryFiles.remove(
                    temporaryFile.absolutePath
                )
            }
        }

    /**
     * Deletes abandoned .part files older than 24 hours.
     *
     * Files belonging to currently active downloads are never deleted.
     */
    private fun cleanupAbandonedTemporaryFiles(
        downloadDirectory: File
    ) {
        val cutoffTime =
            System.currentTimeMillis() - STALE_PART_FILE_AGE_MS

        val temporaryFiles =
            downloadDirectory.listFiles()
                ?: return

        temporaryFiles.forEach { file ->

            if (!file.isFile) return@forEach

            if (!file.name.endsWith(".part")) {
                return@forEach
            }

            if (file.absolutePath in activeTemporaryFiles) {
                return@forEach
            }

            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }

    private fun createFileName(songId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(songId.toByteArray(Charsets.UTF_8))

        val safeId = digest.joinToString("") {
            "%02x".format(it)
        }

        return "$safeId.xenc"
    }
}
