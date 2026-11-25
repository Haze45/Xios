package com.yourapp.music.domain.model

enum class StreamingQuality {
    LOW,
    NORMAL,
    HIGH
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppLanguage(
    val code: String,
    val displayName: String
) {
    companion object {
        val supported = listOf(
            AppLanguage("system", "System default"),
            AppLanguage("en", "English"),
            AppLanguage("hi", "हिन्दी"),
            AppLanguage("bn", "বাংলা"),
            AppLanguage("mr", "मराठी"),
            AppLanguage("ar", "العربية"),
            AppLanguage("zh-CN", "简体中文"),
            AppLanguage("zh-TW", "繁體中文"),
            AppLanguage("es", "Español"),
            AppLanguage("fr", "Français"),
            AppLanguage("pt", "Português"),
            AppLanguage("de", "Deutsch"),
            AppLanguage("ru", "Русский"),
            AppLanguage("ja", "日本語"),
            AppLanguage("ko", "한국어"),
            AppLanguage("id", "Bahasa Indonesia"),
            AppLanguage("ur", "اردو"),
            AppLanguage("pa", "ਪੰਜਾਬੀ"),
            AppLanguage("ta", "தமிழ்"),
            AppLanguage("te", "తెలుగు"),
            AppLanguage("tr", "Türkçe"),
            AppLanguage("vi", "Tiếng Việt"),
            AppLanguage("th", "ไทย"),
            AppLanguage("it", "Italiano"),
            AppLanguage("fa", "فارسی"),
            AppLanguage("sw", "Kiswahili")
        )
    }
}

data class UserSettings(
    val streamingQuality: StreamingQuality = StreamingQuality.NORMAL,
    val playbackNotificationsEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "system"
)
