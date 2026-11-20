package com.yourapp.music.core.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.yourapp.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Name of the subfolder created inside the device's public Downloads folder. */
private const val DOWNLOAD_SUBFOLDER = "MusicApp"

@Singleton
class SongDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Downloads [song]'s audio file and returns the local Uri it was saved to. */
    suspend fun download(song: Song): String =
        withContext(Dispatchers.IO) {
            val bytes = fetchBytes(song.audioUrl)
            val fileName = buildFileName(song)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(fileName, bytes)
            } else {
                saveViaLegacyFile(fileName, bytes)
            }
        }

    private fun fetchBytes(url: String): ByteArray {
        val connection =
            URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Download failed: HTTP ${connection.responseCode}")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildFileName(song: Song): String {
        val safeTitle = song.title.replace(
            Regex("[^A-Za-z0-9 ._-]"),
            "_"
        ).trim()
        val safeArtist = song.artist.replace(
            Regex("[^A-Za-z0-9 ._-]"),
            "_"
        ).trim()
        val base =
            if (safeArtist.isNotBlank()) "$safeArtist - $safeTitle" else safeTitle
        val extension = song.audioUrl.substringBefore('?')
            .substringAfterLast('.', "mp3").take(4)
        return "$base.$extension"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        fileName: String,
        bytes: ByteArray
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "audio/*")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBFOLDER"
            )
        }
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        )
            ?: error("Couldn't create a file in Downloads")
        resolver.openOutputStream(uri)
            ?.use { it.write(bytes) }
            ?: error("Couldn't open an output stream for the new file")
        return uri.toString()
    }

    private fun saveViaLegacyFile(
        fileName: String,
        bytes: ByteArray
    ): String {
        @Suppress("DEPRECATION")
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
        val appDir = File(downloadsDir, DOWNLOAD_SUBFOLDER)
        if (!appDir.exists()) appDir.mkdirs()

        val file = File(appDir, fileName)
        file.writeBytes(bytes)

        // Index the new file so it shows up in the device's Files/Downloads
        // app right away instead of only after the next media scan.
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null
        )

        return Uri.fromFile(file).toString()
    }
}
