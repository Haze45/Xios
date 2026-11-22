package com.yourapp.music.domain.usecase.song

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yourapp.music.domain.model.ListeningHistoryEventType
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.ListeningHistoryRepository
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.GetFavoritesUseCase
import com.yourapp.music.domain.usecase.search.SemanticSearchSongsUseCase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.security.MessageDigest
import javax.inject.Inject

class GetPersonalizedRecommendationsUseCase @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val semanticSearchSongsUseCase: SemanticSearchSongsUseCase,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val getAllSongsUseCase: GetAllSongsUseCase,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val CACHE_DURATION_MS =
            24 * 60 * 60 * 1000L

        private const val MAX_RECOMMENDATIONS = 10
    }

    suspend operator fun invoke(
        forceRefresh: Boolean = false
    ): List<Song> {
        val user =
            getCurrentUserUseCase() ?: return emptyList()

        val favorites =
            getFavoritesUseCase(user.uid).first()

        val history = listeningHistoryRepository
            .observeHistory(user.uid, 50)
            .first()

        val meaningfulHistory = history
            .filter { entry ->
                when (entry.eventType) {
                    ListeningHistoryEventType.COMPLETED -> true

                    ListeningHistoryEventType.SKIPPED ->
                        entry.durationMs > 0L &&
                                entry.listenedMs.toDouble() /
                                entry.durationMs >= 0.60

                    ListeningHistoryEventType.PLAYING,
                    ListeningHistoryEventType.INTERRUPTED ->
                        entry.listenedMs >= 30_000L
                }
            }
            .distinctBy { it.songId }

        if (favorites.isEmpty() && meaningfulHistory.isEmpty()) {
            clearCache(user.uid)
            return emptyList()
        }

        val fingerprint = createPreferenceFingerprint(
            favoriteIds = favorites.map { it.id },
            historySessionIds = meaningfulHistory.map { it.sessionId }
        )

        // Return cached recommendations if they are still valid.
        if (!forceRefresh) {
            val cachedSongs = readCache(
                userId = user.uid,
                fingerprint = fingerprint
            )

            if (cachedSongs != null) {
                return cachedSongs
            }
        }

        val favoriteDescriptions = favorites
            .take(5)
            .map { song ->
                buildString {
                    append(song.title)
                    append(" by ")
                    append(song.artist)

                    if (song.genre.isNotBlank()) {
                        append(", genre ")
                        append(song.genre)
                    }
                }
            }

        val historyDescriptions = meaningfulHistory
            .take(5)
            .map { entry ->
                "${entry.title} by ${entry.artist}"
            }

        val preferenceDescription = buildString {
            if (favoriteDescriptions.isNotEmpty()) {
                append("Favorite songs: ")
                append(favoriteDescriptions.joinToString("; "))
                append(". ")
            }

            if (historyDescriptions.isNotEmpty()) {
                append("Songs listened to meaningfully: ")
                append(historyDescriptions.joinToString("; "))
            }
        }.take(500)

        val query = """
            Recommend music based on these listening preferences.
            Find songs with similar musical styles, artists, genres,
            moods and characteristics.
            $preferenceDescription
        """.trimIndent()

        val semanticMatches = semanticSearchSongsUseCase(
            query = query,
            matchCount = 30
        )

        val excludedIds = buildSet {
            favorites.forEach { add(it.id) }
            history.forEach { add(it.songId) }
        }

        val recommendations = semanticMatches
            .filter { it.song.id !in excludedIds }
            .distinctBy { it.song.id }
            .sortedByDescending {
                it.similarity ?: Double.NEGATIVE_INFINITY
            }
            .take(MAX_RECOMMENDATIONS)
            .map { it.song }

        saveCache(
            userId = user.uid,
            fingerprint = fingerprint,
            songs = recommendations
        )

        return recommendations
    }

    private suspend fun readCache(
        userId: String,
        fingerprint: String
    ): List<Song>? {
        val preferences = dataStore.data.first()

        val cachedFingerprint = preferences[
            stringPreferencesKey("recommendation_fingerprint_$userId")
        ]

        val cachedAt = preferences[
            longPreferencesKey("recommendation_cached_at_$userId")
        ] ?: 0L

        val cachedIdsJson = preferences[
            stringPreferencesKey("recommendation_song_ids_$userId")
        ] ?: return null

        val isFresh =
            System.currentTimeMillis() - cachedAt < CACHE_DURATION_MS

        if (!isFresh || cachedFingerprint != fingerprint) {
            return null
        }

        val cachedIds = runCatching {
            val json = JSONArray(cachedIdsJson)
            List(json.length()) { index ->
                json.getString(index)
            }
        }.getOrNull() ?: return null

        if (cachedIds.isEmpty()) return null

        // Resolve cached IDs from the local Room song cache.
        val localSongs = getAllSongsUseCase().first()
            .associateBy { it.id }

        val resolvedSongs =
            cachedIds.mapNotNull { localSongs[it] }

        // If any cached song is missing, regenerate the recommendations.
        if (resolvedSongs.size != cachedIds.size) {
            return null
        }

        return resolvedSongs
    }

    private suspend fun saveCache(
        userId: String,
        fingerprint: String,
        songs: List<Song>
    ) {
        val idsJson = JSONArray().apply {
            songs.forEach { put(it.id) }
        }.toString()

        dataStore.edit { preferences ->
            preferences[
                stringPreferencesKey("recommendation_fingerprint_$userId")
            ] = fingerprint

            preferences[
                stringPreferencesKey("recommendation_song_ids_$userId")
            ] = idsJson

            preferences[
                longPreferencesKey("recommendation_cached_at_$userId")
            ] = System.currentTimeMillis()
        }
    }

    private suspend fun clearCache(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(
                stringPreferencesKey("recommendation_fingerprint_$userId")
            )
            preferences.remove(
                stringPreferencesKey("recommendation_song_ids_$userId")
            )
            preferences.remove(
                longPreferencesKey("recommendation_cached_at_$userId")
            )
        }
    }

    private fun createPreferenceFingerprint(
        favoriteIds: List<String>,
        historySessionIds: List<String>
    ): String {
        val input = buildString {
            append("favorites:")
            append(favoriteIds.sorted().joinToString(","))
            append("|history:")
            append(historySessionIds.joinToString(","))
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}
