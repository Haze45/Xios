package com.yourapp.music.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class LastPlaybackState(
    val songId: String,
    val queueSongIds: List<String>,
    val positionMs: Long
)

/**
 * Persists just enough about the last thing that was playing (which song,
 * what queue, how far into it) so the MiniPlayer can show it again the
 * moment the app process restarts — before any actual playback has been
 * re-cued into ExoPlayer. Reuses the same Preferences DataStore instance
 * as SettingsDataStore, just under different keys.
 */
@Singleton
class PlaybackStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val LAST_SONG_ID = stringPreferencesKey("last_song_id")
        val LAST_QUEUE_SONG_IDS_JSON = stringPreferencesKey("last_queue_song_ids_json")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
    }

    suspend fun saveLastPlayed(songId: String, queueSongIds: List<String>, positionMs: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_ID] = songId
            prefs[Keys.LAST_QUEUE_SONG_IDS_JSON] = Json.encodeToString(queueSongIds)
            prefs[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    /** Cheap update called on pause, so position stays reasonably fresh without rewriting the whole queue each time. */
    suspend fun savePosition(positionMs: Long) {
        dataStore.edit { prefs -> prefs[Keys.LAST_POSITION_MS] = positionMs }
    }

    suspend fun getLastPlayed(): LastPlaybackState? {
        val prefs = dataStore.data.first()
        val songId = prefs[Keys.LAST_SONG_ID] ?: return null
        val queueIds = prefs[Keys.LAST_QUEUE_SONG_IDS_JSON]
            ?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()
        val positionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L
        return LastPlaybackState(songId, queueIds, positionMs)
    }
}
