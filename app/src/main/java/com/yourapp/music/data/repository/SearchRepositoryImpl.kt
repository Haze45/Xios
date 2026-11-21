package com.yourapp.music.data.repository

import com.yourapp.music.BuildConfig
import com.yourapp.music.data.local.dao.RecentSearchDao
import com.yourapp.music.data.local.entity.RecentSearchEntity
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.remote.firebase.FirebaseAuthService
import com.yourapp.music.data.remote.supabase.dto.SemanticSearchResponseDto
import com.yourapp.music.domain.model.RecentSearch
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val recentSearchDao: RecentSearchDao,
    private val firebaseAuthService: FirebaseAuthService,
    private val httpClient: OkHttpClient
) : SearchRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun getRecentSearches(): Flow<List<RecentSearch>> =
        recentSearchDao.getRecentSearches()
            .map { list -> list.map { it.toDomain() } }

    override suspend fun saveRecentSearch(query: String) {
        recentSearchDao.upsertSearch(
            RecentSearchEntity(
                query = query,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearRecentSearches() =
        recentSearchDao.clearAll()

    override suspend fun semanticSearch(
        query: String,
        matchCount: Int
    ): List<Song> {
        val cleanedQuery = query.trim()

        require(cleanedQuery.isNotBlank()) {
            "Search query cannot be empty"
        }

        require(cleanedQuery.length <= 300) {
            "Search query must be 300 characters or fewer"
        }

        val firebaseUser = firebaseAuthService.currentUser()
            ?: throw IOException("Please sign in to search for songs.")

        val idToken = firebaseUser.getIdToken(false)
            .await()
            .token
            ?: throw IOException("Could not obtain Firebase authentication token.")

        return withContext(Dispatchers.IO) {
            val requestBody = JSONObject()
                .put("query", cleanedQuery)
                .put(
                    "matchCount",
                    matchCount.coerceIn(1, 30)
                )
                .put("matchThreshold", 0.35)
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8".toMediaType()
                )

            val functionUrl =
                "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/semantic-search"

            val request = Request.Builder()
                .url(functionUrl)
                .header(
                    "apikey",
                    BuildConfig.SUPABASE_ANON_KEY
                )
                .header("Authorization", "Bearer $idToken")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute()
                .use { response ->
                    val responseBody =
                        response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        throw IOException(
                            "Semantic search request failed " +
                                    "(${response.code}): $responseBody"
                        )
                    }

                    val result = try {
                        json.decodeFromString<SemanticSearchResponseDto>(
                            responseBody
                        )
                    } catch (exception: Exception) {
                        throw IOException(
                            "Could not read semantic search response.",
                            exception
                        )
                    }

                    if (!result.success) {
                        throw IOException(
                            result.error
                                ?: "Semantic search failed."
                        )
                    }

                    result.results.map { it.toDomain() }
                }
        }
    }
}
