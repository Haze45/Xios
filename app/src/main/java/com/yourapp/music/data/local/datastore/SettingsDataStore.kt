package com.yourapp.music.data.local.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.DataStore
import com.yourapp.music.domain.model.StreamingQuality
import com.yourapp.music.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the Preferences DataStore instance provided by
 * DataStoreModule. Keeps all raw Preferences.Key references isolated here
 * so SettingsRepositoryImpl only ever deals in domain types.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization")
        val PLAYBACK_NOTIFICATIONS = booleanPreferencesKey("playback_notifications")
        val GENERAL_NOTIFICATIONS = booleanPreferencesKey("general_notifications")
        val GRANULAR_NOTIFICATIONS = booleanPreferencesKey("granular_notifications")
    }

    val userSettings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            streamingQuality = prefs[Keys.STREAMING_QUALITY]
                ?.let { runCatching { StreamingQuality.valueOf(it) }.getOrNull() }
                ?: StreamingQuality.NORMAL,
            volumeNormalizationEnabled = prefs[Keys.VOLUME_NORMALIZATION] ?: true,
            playbackNotificationsEnabled = prefs[Keys.PLAYBACK_NOTIFICATIONS] ?: true,
            generalNotificationsEnabled = prefs[Keys.GENERAL_NOTIFICATIONS] ?: false,
            granularNotificationsEnabled = prefs[Keys.GRANULAR_NOTIFICATIONS] ?: true
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.STREAMING_QUALITY] = settings.streamingQuality.name
            prefs[Keys.VOLUME_NORMALIZATION] = settings.volumeNormalizationEnabled
            prefs[Keys.PLAYBACK_NOTIFICATIONS] = settings.playbackNotificationsEnabled
            prefs[Keys.GENERAL_NOTIFICATIONS] = settings.generalNotificationsEnabled
            prefs[Keys.GRANULAR_NOTIFICATIONS] = settings.granularNotificationsEnabled
        }
    }
}
