package com.yourapp.music.domain.model

data class DownloadProgress(
    val song: Song,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val status: String = "RUNNING"
) {
    val fraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0L }
            ?.let {
                (downloadedBytes.toFloat() / it)
                    .coerceIn(0f, 1f)
            }

    val percentage: Int?
        get() = fraction?.let {
            (it * 100).toInt()
        }

    val isPaused: Boolean
        get() = status == "PAUSED"

    val isFailed: Boolean
        get() = status == "FAILED"
}
