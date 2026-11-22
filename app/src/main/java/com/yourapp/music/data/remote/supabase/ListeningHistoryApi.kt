package com.yourapp.music.data.remote.supabase

import com.yourapp.music.BuildConfig
import com.yourapp.music.data.local.entity.ListeningHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningHistoryApi @Inject constructor(
    private val httpClient: OkHttpClient
) {

    suspend fun syncHistory(
        events: List<ListeningHistoryEntity>,
        firebaseIdToken: String
    ) {
        if (events.isEmpty()) return

        require(events.size <= 100) {
            "A maximum of 100 history events can be synced at once."
        }

        withContext(Dispatchers.IO) {
            val eventsJson = JSONArray()

            events.forEach { event ->
                eventsJson.put(
                    JSONObject().apply {
                        put("session_id", event.sessionId)
                        put("song_id", event.songId)
                        put("title", event.title)
                        put("artist", event.artist)
                        put(
                            "album_art_url",
                            event.albumArtUrl
                        )
                        put("played_at_ms", event.playedAt)
                        put("duration_ms", event.durationMs)
                        put("listened_ms", event.listenedMs)
                        put(
                            "last_position_ms",
                            event.lastPositionMs
                        )
                        put("event_type", event.eventType)
                    }
                )
            }

            val requestBody = JSONObject()
                .put("events", eventsJson)
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )

            val functionUrl =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}" +
                        "/functions/v1/sync-listening-history"

            val request = Request.Builder()
                .url(functionUrl)
                .header(
                    "apikey",
                    BuildConfig.SUPABASE_ANON_KEY
                )
                .header(
                    "Authorization",
                    "Bearer $firebaseIdToken"
                )
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute()
                .use { response ->
                    val responseBody =
                        response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        throw IOException(
                            "Listening history sync failed " +
                                    "(${response.code}): $responseBody"
                        )
                    }

                    val success = runCatching {
                        JSONObject(responseBody).optBoolean(
                            "success"
                        )
                    }.getOrDefault(false)

                    if (!success) {
                        throw IOException(
                            "The server did not confirm listening history sync."
                        )
                    }
                }
        }
    }
}
