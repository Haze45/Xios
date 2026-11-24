package com.yourapp.music.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class DownloadTaskEntity(
    @PrimaryKey
    val songId: String,

    val temporaryFilePath: String,

    val downloadedBytes: Long = 0L,

    val totalBytes: Long? = null,

    val status: String = "QUEUED",

    val updatedAt: Long = System.currentTimeMillis(),

    // Remote source validation.
    val sourceETag: String? = null,
    val sourceLastModified: String? = null,
    val sourceLength: Long? = null,
    val sourceUrlHash: String? = null,

    // Encryption resume metadata.
    val encryptionSalt: String? = null,
    val chunkCount: Int = 0
)