package com.yourapp.music.core.download

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ResumableSongEncryption private constructor(
    private val file: RandomAccessFile,
    private val key: SecretKey,
    val salt: ByteArray,
    private var committedChunkCount: Int
) : Closeable {

    companion object {
        private const val KEYSTORE_PROVIDER =
            "AndroidKeyStore"
        private const val KEY_ALIAS =
            "music_app_download_key_v1"
        private const val TAG_LENGTH_BITS = 128

        private const val CHUNK_SIZE = 64 * 1024
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 16
        private const val SALT_LENGTH = 16

        private const val HEADER_PREFIX_LENGTH = 32
        private const val HEADER_FIELDS_LENGTH = 44
        private const val HEADER_SIZE = 72

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private val secureRandom = SecureRandom()

        private fun getOrCreateKey(): SecretKey {
            val keyStore = KeyStore.getInstance(
                KEYSTORE_PROVIDER
            ).apply {
                load(null)
            }

            val existing = keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

            if (existing != null) return existing

            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                    )
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)
                    .build()
            )

            return generator.generateKey()
        }

        /**
         * Opens an existing partial file or creates a new one.
         *
         * Only complete, authenticated chunks are retained.
         * An incomplete or invalid trailing record is discarded.
         */
        fun openOrCreate(
            destination: File,
            expectedSalt: ByteArray? = null
        ): ResumableSongEncryption {
            require(
                expectedSalt == null ||
                        expectedSalt.size == SALT_LENGTH
            ) {
                "Invalid encryption salt length."
            }

            destination.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException(
                        "Couldn't create encrypted download directory."
                    )
                }
            }

            val key = getOrCreateKey()
            val randomAccessFile = RandomAccessFile(
                destination,
                "rw"
            )

            try {
                if (randomAccessFile.length() == 0L) {
                    val salt = expectedSalt?.copyOf()
                        ?: ByteArray(SALT_LENGTH).also {
                            secureRandom.nextBytes(it)
                        }

                    val prefix = createHeaderPrefix(salt)

                    randomAccessFile.write(prefix)
                    randomAccessFile.write(
                        ByteArray(HEADER_SIZE - prefix.size)
                    )
                    randomAccessFile.fd.sync()

                    return ResumableSongEncryption(
                        file = randomAccessFile,
                        key = key,
                        salt = salt,
                        committedChunkCount = 0
                    )
                }

                if (randomAccessFile.length() < HEADER_SIZE) {
                    throw IOException(
                        "Partial encrypted header is corrupted."
                    )
                }

                randomAccessFile.seek(0)

                val prefix = ByteArray(
                    HEADER_PREFIX_LENGTH
                )

                randomAccessFile.readFully(prefix)

                validatePrefix(prefix)

                val storedSalt = prefix.copyOfRange(
                    16,
                    16 + SALT_LENGTH
                )

                if (
                    expectedSalt != null &&
                    !storedSalt.contentEquals(expectedSalt)
                ) {
                    throw IOException(
                        "Download encryption salt doesn't match."
                    )
                }

                val recordSize =
                    CHUNK_SIZE.toLong() +
                            IV_LENGTH +
                            TAG_LENGTH

                val availableBytes =
                    randomAccessFile.length() - HEADER_SIZE

                var completeRecords =
                    (availableBytes / recordSize).toInt()

                // Retain only fully written records.
                var validRecords = 0

                while (validRecords < completeRecords) {
                    try {
                        verifyChunk(
                            file = randomAccessFile,
                            key = key,
                            prefix = prefix,
                            index = validRecords
                        )
                        validRecords++
                    } catch (_: Exception) {
                        break
                    }
                }

                // Discard any incomplete or unauthenticated tail.
                val validLength =
                    HEADER_SIZE.toLong() +
                            validRecords.toLong() * recordSize

                if (randomAccessFile.length() != validLength) {
                    randomAccessFile.setLength(validLength)
                    randomAccessFile.fd.sync()
                }

                completeRecords = validRecords

                return ResumableSongEncryption(
                    file = randomAccessFile,
                    key = key,
                    salt = storedSalt,
                    committedChunkCount = completeRecords
                )

            } catch (exception: Exception) {
                randomAccessFile.close()

                if (exception is IOException) throw exception

                throw IOException(
                    "Couldn't reopen encrypted partial download.",
                    exception
                )
            }
        }

        private fun verifyChunk(
            file: RandomAccessFile,
            key: SecretKey,
            prefix: ByteArray,
            index: Int
        ) {
            val recordSize =
                CHUNK_SIZE.toLong() + IV_LENGTH + TAG_LENGTH

            val offset =
                HEADER_SIZE.toLong() +
                        index.toLong() * recordSize

            val iv = ByteArray(IV_LENGTH)
            val encrypted = ByteArray(
                CHUNK_SIZE + TAG_LENGTH
            )

            try {
                file.seek(offset)
                file.readFully(iv)
                file.readFully(encrypted)

                val cipher = Cipher.getInstance(
                    TRANSFORMATION
                )

                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_LENGTH_BITS, iv)
                )

                cipher.updateAAD(
                    createChunkAad(
                        prefix,
                        index,
                        CHUNK_SIZE
                    )
                )

                val plaintext = cipher.doFinal(encrypted)

                try {
                    if (plaintext.size != CHUNK_SIZE) {
                        throw IOException(
                            "Invalid encrypted chunk length."
                        )
                    }
                } finally {
                    plaintext.fill(0)
                }
            } finally {
                iv.fill(0)
                encrypted.fill(0)
            }
        }

        private fun validatePrefix(prefix: ByteArray) {
            val buffer = ByteBuffer.wrap(prefix)

            val magic = ByteArray(8)
            buffer.get(magic)

            if (
                !magic.contentEquals(
                    "MUSCHN01".toByteArray(Charsets.UTF_8)
                )
            ) {
                throw IOException(
                    "Unsupported encrypted file format."
                )
            }

            val version = buffer.int
            val chunkSize = buffer.int

            if (version != 1 || chunkSize != CHUNK_SIZE) {
                throw IOException(
                    "Unsupported encrypted file version."
                )
            }
        }

        private fun createHeaderPrefix(
            salt: ByteArray
        ): ByteArray {
            return ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.write(
                        "MUSCHN01".toByteArray(Charsets.UTF_8)
                    )
                    output.writeInt(1)
                    output.writeInt(CHUNK_SIZE)
                    output.write(salt)
                }
                bytes.toByteArray()
            }
        }

        private fun createChunkAad(
            prefix: ByteArray,
            index: Int,
            chunkLength: Int
        ): ByteArray {
            return ByteBuffer.allocate(prefix.size + 8)
                .put(prefix)
                .putInt(index)
                .putInt(chunkLength)
                .array()
        }
    }

    /**
     * Number of plaintext bytes already committed to disk.
     *
     * For an unfinished file this is always a multiple of 64 KiB.
     */
    val resumeOffsetBytes: Long
        get() = committedChunkCount.toLong() * CHUNK_SIZE

    val chunkCount: Int
        get() = committedChunkCount

    /**
     * Reads the remaining HTTP response.
     *
     * Full chunks are committed and synced individually.
     * A final short chunk is written only when EOF is reached.
     * If the coroutine is cancelled between chunks, the partial
     * file remains resumable.
     */
    suspend fun append(
        input: InputStream,
        onChunkCommitted: suspend  (downloadedBytes: Long) -> Unit = {}
    ): Long = withContext(Dispatchers.IO) {
        val prefix = createHeaderPrefix(salt)

        val buffer = ByteArray(CHUNK_SIZE)
        var totalLength = resumeOffsetBytes
        var finalized = false

        try {
            while (true) {
                currentCoroutineContext().ensureActive()

                val bytesRead = readChunk(input, buffer)

                if (bytesRead == 0) {
                    finalizeHeader(
                        plaintextLength = totalLength,
                        chunkCount = committedChunkCount,
                        prefix = prefix
                    )
                    finalized = true
                    break
                }

                // A full chunk can be safely committed immediately.
                // A short chunk is the final record and is only
                // committed when the input reaches EOF.
                if (bytesRead == CHUNK_SIZE) {
                    writeChunk(
                        prefix = prefix,
                        index = committedChunkCount,
                        plaintext = buffer,
                        length = bytesRead
                    )

                    totalLength = Math.addExact(
                        totalLength,
                        bytesRead.toLong()
                    )

                    committedChunkCount++
                    onChunkCommitted(totalLength)

                    buffer.fill(0)
                } else {
                    // We have reached EOF after this short read.
                    writeChunk(
                        prefix = prefix,
                        index = committedChunkCount,
                        plaintext = buffer,
                        length = bytesRead
                    )

                    totalLength = Math.addExact(
                        totalLength,
                        bytesRead.toLong()
                    )

                    committedChunkCount++
                    onChunkCommitted(totalLength)

                    buffer.fill(0)

                    finalizeHeader(
                        plaintextLength = totalLength,
                        chunkCount = committedChunkCount,
                        prefix = prefix
                    )

                    finalized = true
                    break
                }
            }

            totalLength

        } finally {
            buffer.fill(0)

            // Do not delete or truncate the partial file here.
            // The caller may resume it later.
            if (!finalized) {
                file.fd.sync()
            }
        }
    }

    private fun writeChunk(
        prefix: ByteArray,
        index: Int,
        plaintext: ByteArray,
        length: Int
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // Let Android Keystore generate a fresh IV.
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key
        )

        val iv = cipher.iv
            ?: throw IOException(
                "Couldn't generate encryption IV."
            )

        if (iv.size != IV_LENGTH) {
            throw IOException("Invalid encryption IV length.")
        }

        cipher.updateAAD(
            createChunkAad(
                prefix,
                index,
                length
            )
        )

        val encrypted = cipher.doFinal(
            plaintext,
            0,
            length
        )

        try {
            val recordSize =
                CHUNK_SIZE.toLong() + IV_LENGTH + TAG_LENGTH

            val offset =
                HEADER_SIZE.toLong() +
                        index.toLong() * recordSize

            file.seek(offset)
            file.write(iv)
            file.write(encrypted)
            file.fd.sync()
        } finally {
            iv.fill(0)
            encrypted.fill(0)
        }
    }

    private fun finalizeHeader(
        plaintextLength: Long,
        chunkCount: Int,
        prefix: ByteArray
    ) {
        val fields = ByteBuffer.allocate(
            HEADER_FIELDS_LENGTH
        )
            .put(prefix)
            .putLong(plaintextLength)
            .putInt(chunkCount)
            .array()

        val cipher = Cipher.getInstance(TRANSFORMATION)

        // A fresh Keystore-generated IV is used for the header.
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key
        )

        val iv = cipher.iv
            ?: throw IOException(
                "Couldn't generate header IV."
            )

        cipher.updateAAD(fields)

        // GCM over empty plaintext produces the authentication tag.
        val tag = cipher.doFinal()

        try {
            file.seek(0)
            file.write(fields)
            file.write(iv)
            file.write(tag)
            file.fd.sync()
        } finally {
            fields.fill(0)
            iv.fill(0)
            tag.fill(0)
        }
    }

    private fun readChunk(
        input: InputStream,
        buffer: ByteArray
    ): Int {
        var totalRead = 0

        while (totalRead < buffer.size) {
            val count = input.read(
                buffer,
                totalRead,
                buffer.size - totalRead
            )

            if (count < 0) break

            if (count == 0) {
                val single = input.read()
                if (single < 0) break

                buffer[totalRead++] = single.toByte()
            } else {
                totalRead += count
            }
        }

        return totalRead
    }

    override fun close() {
        salt.fill(0)
        file.close()
    }
}
