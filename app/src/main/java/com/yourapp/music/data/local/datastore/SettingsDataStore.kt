package com.yourapp.music.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yourapp.music.domain.model.AppTheme
import com.yourapp.music.domain.model.StreamingQuality
import com.yourapp.music.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val STREAMING_QUALITY =
            stringPreferencesKey("streaming_quality")

        val PLAYBACK_NOTIFICATIONS =
            booleanPreferencesKey("playback_notifications")

        val THEME =
            stringPreferencesKey("app_theme")

        val LANGUAGE =
            stringPreferencesKey("app_language")
    }

    val userSettings: Flow<UserSettings> =
        dataStore.data.map { prefs ->
            UserSettings(
                streamingQuality = prefs[Keys.STREAMING_QUALITY]
                    ?.let {
                        runCatching {
                            StreamingQuality.valueOf(it)
                        }.getOrNull()
                    } ?: StreamingQuality.NORMAL,

                playbackNotificationsEnabled =
                    prefs[Keys.PLAYBACK_NOTIFICATIONS]
                        ?: true,

                theme = prefs[Keys.THEME]
                    ?.let {
                        runCatching {
                            AppTheme.valueOf(it)
                        }.getOrNull()
                    } ?: AppTheme.SYSTEM,

                language = prefs[Keys.LANGUAGE] ?: "system"
            )
        }

    suspend fun updateSettings(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.STREAMING_QUALITY] =
                settings.streamingQuality.name

            prefs[Keys.PLAYBACK_NOTIFICATIONS] =
                settings.playbackNotificationsEnabled

            prefs[Keys.THEME] =
                settings.theme.name

            prefs[Keys.LANGUAGE] =
                settings.language
        }
    }
}
