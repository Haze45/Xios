package com.yourapp.music.domain.model

enum class StreamingQuality { LOW, NORMAL, HIGH } // "Highbaots"-style dropdown in the design maps to these

data class UserSettings(
    val streamingQuality: StreamingQuality = StreamingQuality.NORMAL,
    val volumeNormalizationEnabled: Boolean = true,
    val playbackNotificationsEnabled: Boolean = true,
    val generalNotificationsEnabled: Boolean = false,
    val granularNotificationsEnabled: Boolean = true
)
