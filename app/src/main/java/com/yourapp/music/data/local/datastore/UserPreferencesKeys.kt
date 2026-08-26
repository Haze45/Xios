package com.yourapp.music.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Keys for the Preferences DataStore backing app settings (streaming quality, notifications, etc). */
object UserPreferencesKeys {
    val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
    val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
    val VOLUME_NORMALIZATION = booleanPreferencesKey("volume_normalization")
    val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
    val NEW_RELEASE_ALERTS = booleanPreferencesKey("new_release_alerts")
    val PLAYLIST_UPDATE_ALERTS = booleanPreferencesKey("playlist_update_alerts")
    val FOLLOWER_ALERTS = booleanPreferencesKey("follower_alerts")
}
