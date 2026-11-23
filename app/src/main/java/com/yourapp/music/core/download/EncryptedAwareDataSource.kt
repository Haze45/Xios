package com.yourapp.music.core.download

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import java.io.File
import java.io.IOException

@UnstableApi
class EncryptedAwareDataSourceFactory(
    context: Context,
    private val encryptionManager: SongEncryptionManager
) : DataSource.Factory {

    private val defaultFactory =
        DefaultDataSource.Factory(context)

    override fun createDataSource(): DataSource {
        return EncryptedAwareDataSource(
            defaultDataSource = defaultFactory.createDataSource(),
            encryptedDataSource = EncryptedFileDataSource(
                encryptionManager
            )
        )
    }
}

@UnstableApi
private class EncryptedAwareDataSource(
    private val defaultDataSource: DataSource,
    private val encryptedDataSource: DataSource
) : DataSource {

    private var activeDataSource: DataSource? = null

    override fun addTransferListener(
        transferListener: TransferListener
    ) {
        defaultDataSource.addTransferListener(
            transferListener
        )
    }

    override fun open(dataSpec: DataSpec): Long {
        val selectedDataSource =
            if (dataSpec.uri.scheme == "xios-encrypted") {
                encryptedDataSource
            } else {
                defaultDataSource
            }

        activeDataSource = selectedDataSource
        return selectedDataSource.open(dataSpec)
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        return activeDataSource?.read(
            buffer,
            offset,
            length
        ) ?: C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri? =
        activeDataSource?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        activeDataSource?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            activeDataSource?.close()
        } finally {
            activeDataSource = null
        }
    }
}

@UnstableApi
private class EncryptedFileDataSource(
    private val encryptionManager: SongEncryptionManager
) : DataSource {

    private var currentUri: Uri? = null
    private var reader: EncryptedSongReader? = null

    private var currentChunk: ByteArray? = null
    private var currentChunkOffset = 0

    private var firstChunkOffset = 0
    private var nextChunkIndex = 0

    private var bytesRemaining = 0L

    override fun addTransferListener(
        transferListener: TransferListener
    ) = Unit

    override fun open(dataSpec: DataSpec): Long {
        close()

        val path = dataSpec.uri.path
            ?: throw IOException("Encrypted song path is missing.")

        val encryptedFile = File(path)

        if (!encryptedFile.exists() || !encryptedFile.isFile) {
            throw IOException("Downloaded song file doesn't exist.")
        }

        val openedReader = encryptionManager.openReader(
            encryptedFile
        )

        try {
            val metadata = openedReader.metadata
            val requestedPosition = dataSpec.position

            if (requestedPosition < 0 ||
                requestedPosition > metadata.plaintextLength
            ) {
                throw IOException("Invalid playback position.")
            }

            val availableBytes =
                metadata.plaintextLength - requestedPosition

            bytesRemaining =
                if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                    availableBytes
                } else {
                    if (dataSpec.length < 0) {
                        throw IOException("Invalid requested data length.")
                    }

                    minOf(availableBytes, dataSpec.length)
                }

            nextChunkIndex = (
                    requestedPosition / metadata.chunkSize
                    ).toInt()

            firstChunkOffset = (
                    requestedPosition % metadata.chunkSize
                    ).toInt()

            currentChunkOffset = 0
            currentUri = dataSpec.uri
            reader = openedReader

            return bytesRemaining

        } catch (exception: Exception) {
            openedReader.close()

            if (exception is IOException) throw exception

            throw IOException(
                "Couldn't open encrypted audio.",
                exception
            )
        }
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        if (length == 0) return 0

        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val activeReader = reader
            ?: throw IOException("Encrypted audio is not open.")

        var copiedBytes = 0

        val requestedBytes = minOf(
            length.toLong(),
            bytesRemaining
        ).toInt()

        while (copiedBytes < requestedBytes) {

            if (currentChunk == null ||
                currentChunkOffset >= currentChunk!!.size
            ) {
                // Clear the previous plaintext chunk before loading another.
                currentChunk?.fill(0)
                currentChunk = null

                if (nextChunkIndex >=
                    activeReader.metadata.chunkCount
                ) {
                    throw IOException(
                        "Unexpected end of encrypted audio."
                    )
                }

                currentChunk = activeReader.decryptChunk(
                    nextChunkIndex
                )

                nextChunkIndex++

                currentChunkOffset = firstChunkOffset
                firstChunkOffset = 0
            }

            val source = currentChunk
                ?: throw IOException("Couldn't decrypt audio chunk.")

            val availableInChunk =
                source.size - currentChunkOffset

            val bytesToCopy = minOf(
                requestedBytes - copiedBytes,
                availableInChunk
            )

            if (bytesToCopy <= 0) break

            source.copyInto(
                destination = buffer,
                destinationOffset = offset + copiedBytes,
                startIndex = currentChunkOffset,
                endIndex = currentChunkOffset + bytesToCopy
            )

            currentChunkOffset += bytesToCopy
            copiedBytes += bytesToCopy
            bytesRemaining -= bytesToCopy
        }

        return if (copiedBytes == 0) {
            C.RESULT_END_OF_INPUT
        } else {
            copiedBytes
        }
    }

    override fun getUri(): Uri? = currentUri

    override fun getResponseHeaders(): Map<String, List<String>> =
        emptyMap()

    override fun close() {
        currentChunk?.fill(0)
        currentChunk = null

        reader?.close()
        reader = null

        currentUri = null
        currentChunkOffset = 0
        firstChunkOffset = 0
        nextChunkIndex = 0
        bytesRemaining = 0L
    }
}
