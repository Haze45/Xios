package com.yourapp.music.core.download

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

internal object EncryptedSongFormat {
    val MAGIC = "MUSCHN01".toByteArray(Charsets.UTF_8)

    const val VERSION = 1
    const val CHUNK_SIZE = 64 * 1024

    const val IV_LENGTH = 12
    const val TAG_LENGTH = 16
    const val SALT_LENGTH = 16

    const val HEADER_PREFIX_LENGTH = 32
    const val HEADER_FIELDS_LENGTH = 44
    const val HEADER_SIZE = 72

    const val TRANSFORMATION = "AES/GCM/NoPadding"
}

class EncryptedSongMetadata(
    val plaintextLength: Long,
    val chunkSize: Int,
    val chunkCount: Int,
    internal val chunkAadPrefix: ByteArray
)

@Singleton
class SongEncryptionManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER =
            "AndroidKeyStore"
        private const val KEY_ALIAS =
            "music_app_download_key_v1"
        private const val TAG_LENGTH_BITS = 128
    }

    private val secureRandom = SecureRandom()

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey

        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        keyGenerator.init(
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

        return keyGenerator.generateKey()
    }

    suspend fun encrypt(
        input: InputStream,
        destination: File
    ): Long = withContext(Dispatchers.IO) {

        val key = getOrCreateKey()

        val salt =
            ByteArray(EncryptedSongFormat.SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val aadPrefix = createHeaderPrefix(salt)

        RandomAccessFile(destination, "rw").use { output ->

            output.setLength(0)

            // Reserve space for the authenticated header.
            output.write(
                ByteArray(EncryptedSongFormat.HEADER_SIZE)
            )

            val plainBuffer =
                ByteArray(EncryptedSongFormat.CHUNK_SIZE)

            var totalLength = 0L
            var chunkCount = 0

            try {
                while (true) {

                    currentCoroutineContext().ensureActive()

                    val bytesRead = readChunk(
                        input = input,
                        buffer = plainBuffer
                    )

                    if (bytesRead == 0) break

                    if (chunkCount == Int.MAX_VALUE) {
                        throw IOException("Too many audio chunks.")
                    }

                    // Android Keystore generates the IV.
                    val cipher = Cipher.getInstance(
                        EncryptedSongFormat.TRANSFORMATION
                    )

                    cipher.init(
                        Cipher.ENCRYPT_MODE,
                        key
                    )

                    val iv = cipher.iv
                        ?: throw IOException(
                            "Unable to generate encryption IV."
                        )

                    if (iv.size != EncryptedSongFormat.IV_LENGTH) {
                        throw IOException(
                            "Invalid encryption IV length."
                        )
                    }

                    cipher.updateAAD(
                        createChunkAad(
                            prefix = aadPrefix,
                            index = chunkCount,
                            chunkLength = bytesRead
                        )
                    )

                    val encryptedChunk = cipher.doFinal(
                        plainBuffer,
                        0,
                        bytesRead
                    )

                    output.write(iv)
                    output.write(encryptedChunk)

                    encryptedChunk.fill(0)
                    plainBuffer.fill(0, 0, bytesRead)

                    totalLength = Math.addExact(
                        totalLength,
                        bytesRead.toLong()
                    )

                    chunkCount++
                }

                val headerFields = createHeaderFields(
                    prefix = aadPrefix,
                    plaintextLength = totalLength,
                    chunkCount = chunkCount
                )

                // Android Keystore generates the header IV.
                val headerCipher = Cipher.getInstance(
                    EncryptedSongFormat.TRANSFORMATION
                )

                headerCipher.init(
                    Cipher.ENCRYPT_MODE,
                    key
                )

                val headerIv = headerCipher.iv
                    ?: throw IOException(
                        "Unable to generate header IV."
                    )

                if (headerIv.size != EncryptedSongFormat.IV_LENGTH) {
                    throw IOException(
                        "Invalid header IV length."
                    )
                }

                headerCipher.updateAAD(headerFields)

                // Encrypting empty input produces the GCM authentication tag.
                val headerTag = headerCipher.doFinal()

                output.seek(0)
                output.write(headerFields)
                output.write(headerIv)
                output.write(headerTag)
                output.fd.sync()

                headerTag.fill(0)

                totalLength

            } finally {
                plainBuffer.fill(0)
                salt.fill(0)
            }
        }
    }

    /**
     * Opens and authenticates an encrypted file without decrypting
     * its audio content.
     */
    fun openReader(file: File): EncryptedSongReader {

        val randomAccessFile = RandomAccessFile(file, "r")

        try {
            if (
                randomAccessFile.length() <
                EncryptedSongFormat.HEADER_SIZE
            ) {
                throw IOException(
                    "Encrypted song file is incomplete."
                )
            }

            val headerFields =
                ByteArray(EncryptedSongFormat.HEADER_FIELDS_LENGTH)

            randomAccessFile.readFully(headerFields)

            val input = DataInputStream(
                ByteArrayInputStream(headerFields)
            )

            val magic =
                ByteArray(EncryptedSongFormat.MAGIC.size)

            input.readFully(magic)

            if (!magic.contentEquals(EncryptedSongFormat.MAGIC)) {
                throw IOException(
                    "Unsupported encrypted song format."
                )
            }

            val version = input.readInt()
            val chunkSize = input.readInt()

            val salt =
                ByteArray(EncryptedSongFormat.SALT_LENGTH)

            input.readFully(salt)

            val plaintextLength = input.readLong()
            val chunkCount = input.readInt()

            if (version != EncryptedSongFormat.VERSION) {
                throw IOException(
                    "Unsupported encrypted song version."
                )
            }

            if (chunkSize != EncryptedSongFormat.CHUNK_SIZE) {
                throw IOException(
                    "Invalid encrypted chunk size."
                )
            }

            if (plaintextLength < 0 || chunkCount < 0) {
                throw IOException(
                    "Invalid encrypted song metadata."
                )
            }

            val expectedChunkCount =
                if (plaintextLength == 0L) {
                    0L
                } else {
                    ((plaintextLength - 1L) / chunkSize) + 1L
                }

            if (expectedChunkCount != chunkCount.toLong()) {
                throw IOException(
                    "Invalid encrypted chunk count."
                )
            }

            val headerIv =
                ByteArray(EncryptedSongFormat.IV_LENGTH)

            val headerTag =
                ByteArray(EncryptedSongFormat.TAG_LENGTH)

            randomAccessFile.readFully(headerIv)
            randomAccessFile.readFully(headerTag)

            val expectedFileLength = try {
                Math.addExact(
                    EncryptedSongFormat.HEADER_SIZE.toLong(),
                    Math.addExact(
                        plaintextLength,
                        chunkCount.toLong() *
                                (
                                        EncryptedSongFormat.IV_LENGTH +
                                                EncryptedSongFormat.TAG_LENGTH
                                        )
                    )
                )
            } catch (exception: ArithmeticException) {
                throw IOException(
                    "Invalid encrypted song length.",
                    exception
                )
            }

            if (randomAccessFile.length() != expectedFileLength) {
                throw IOException(
                    "Encrypted song file is corrupted."
                )
            }

            val cipher = Cipher.getInstance(
                EncryptedSongFormat.TRANSFORMATION
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    TAG_LENGTH_BITS,
                    headerIv
                )
            )

            cipher.updateAAD(headerFields)

            try {
                val result = cipher.doFinal(headerTag)
                result.fill(0)

            } catch (exception: AEADBadTagException) {
                throw IOException(
                    "Encrypted song header authentication failed.",
                    exception
                )
            }

            return EncryptedSongReader(
                file = randomAccessFile,
                key = getOrCreateKey(),
                metadata = EncryptedSongMetadata(
                    plaintextLength = plaintextLength,
                    chunkSize = chunkSize,
                    chunkCount = chunkCount,
                    chunkAadPrefix = headerFields.copyOf(
                        EncryptedSongFormat.HEADER_PREFIX_LENGTH
                    )
                )
            )

        } catch (exception: Exception) {

            randomAccessFile.close()

            if (exception is IOException) throw exception

            throw IOException(
                "Couldn't open encrypted song.",
                exception
            )
        }
    }

    private fun createHeaderPrefix(
        salt: ByteArray
    ): ByteArray {
        return ByteArrayOutputStream().use { bytes ->

            DataOutputStream(bytes).use { output ->
                output.write(EncryptedSongFormat.MAGIC)
                output.writeInt(EncryptedSongFormat.VERSION)
                output.writeInt(EncryptedSongFormat.CHUNK_SIZE)
                output.write(salt)
            }

            bytes.toByteArray()
        }
    }

    private fun createHeaderFields(
        prefix: ByteArray,
        plaintextLength: Long,
        chunkCount: Int
    ): ByteArray {
        return ByteArrayOutputStream().use { bytes ->

            DataOutputStream(bytes).use { output ->
                output.write(prefix)
                output.writeLong(plaintextLength)
                output.writeInt(chunkCount)
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

                val singleByte = input.read()

                if (singleByte < 0) break

                buffer[totalRead++] = singleByte.toByte()

            } else {
                totalRead += count
            }
        }

        return totalRead
    }
}

class EncryptedSongReader internal constructor(
    private val file: RandomAccessFile,
    private val key: SecretKey,
    val metadata: EncryptedSongMetadata
) : Closeable {

    companion object {
        private const val TAG_LENGTH_BITS = 128
    }

    fun decryptChunk(index: Int): ByteArray {

        if (index !in 0 until metadata.chunkCount) {
            throw IOException(
                "Invalid encrypted chunk index."
            )
        }

        val plaintextOffset =
            index.toLong() * metadata.chunkSize

        val chunkLength = minOf(
            metadata.chunkSize.toLong(),
            metadata.plaintextLength - plaintextOffset
        ).toInt()

        val recordSize =
            metadata.chunkSize.toLong() +
                    EncryptedSongFormat.IV_LENGTH +
                    EncryptedSongFormat.TAG_LENGTH

        val fileOffset =
            EncryptedSongFormat.HEADER_SIZE.toLong() +
                    index.toLong() * recordSize

        val iv =
            ByteArray(EncryptedSongFormat.IV_LENGTH)

        val encryptedChunk = ByteArray(
            chunkLength + EncryptedSongFormat.TAG_LENGTH
        )

        file.seek(fileOffset)
        file.readFully(iv)
        file.readFully(encryptedChunk)

        val cipher = Cipher.getInstance(
            EncryptedSongFormat.TRANSFORMATION
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(TAG_LENGTH_BITS, iv)
        )

        val chunkContext = ByteBuffer.allocate(8)
            .putInt(index)
            .putInt(chunkLength)
            .array()

        cipher.updateAAD(metadata.chunkAadPrefix)
        cipher.updateAAD(chunkContext)

        return try {
            cipher.doFinal(encryptedChunk)

        } catch (exception: Exception) {
            throw IOException(
                "Audio chunk authentication or decryption failed.",
                exception
            )

        } finally {
            encryptedChunk.fill(0)
            iv.fill(0)
        }
    }

    override fun close() {
        file.close()
    }
}
