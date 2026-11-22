package com.yourapp.music.domain.usecase.song

import com.yourapp.music.domain.model.SemanticSongMatch
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.usecase.search.SemanticSearchSongsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject

data class HybridSearchResult(
    val songs: List<Song>,
    val semanticSearchSucceeded: Boolean
)

class HybridSearchSongsUseCase @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val semanticSearchSongsUseCase: SemanticSearchSongsUseCase
) {

    companion object {
        private const val RRF_CONSTANT = 60.0

        private const val LOCAL_RANK_WEIGHT = 1000.0
        private const val SEMANTIC_RANK_WEIGHT = 1000.0

        private const val SIMILARITY_WEIGHT = 25.0
    }

    suspend operator fun invoke(
        query: String
    ): HybridSearchResult {

        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) {
            return HybridSearchResult(
                songs = emptyList(),
                semanticSearchSucceeded = false
            )
        }

        // Step 1: Get Room-backed fuzzy results.
        val localResults =
            searchSongsUseCase(cleanedQuery).first()

        // Step 2: Get Gemini semantic matches with similarity scores.
        val semanticResults = try {
            semanticSearchSongsUseCase(
                query = cleanedQuery,
                matchCount = 30
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return HybridSearchResult(
                songs = localResults,
                semanticSearchSucceeded = false
            )
        }

        // Step 3: Merge and rank.
        return HybridSearchResult(
            songs = mergeAndRank(
                query = cleanedQuery,
                localResults = localResults,
                semanticResults = semanticResults
            ),
            semanticSearchSucceeded = true
        )
    }

    private fun mergeAndRank(
        query: String,
        localResults: List<Song>,
        semanticResults: List<SemanticSongMatch>
    ): List<Song> {

        val normalizedQuery = normalize(query)

        // Build local result positions.
        val localRanks = localResults
            .mapIndexed { index, song ->
                song.id to index
            }
            .toMap()

        // Build semantic result positions.
        val semanticRanks = semanticResults
            .mapIndexed { index, match ->
                match.song.id to index
            }
            .toMap()

        // Preserve the actual Gemini similarity score.
        val similarityScores = semanticResults
            .associate { match ->
                match.song.id to match.similarity
            }

        // Merge without duplicate song IDs.
        val candidates = (
                localResults + semanticResults.map { it.song }
                ).distinctBy { it.id }

        return candidates.sortedWith(
            compareByDescending<Song> { song ->

                val localRank = localRanks[song.id]
                val semanticRank = semanticRanks[song.id]

                // Reciprocal-rank fusion for local fuzzy results.
                val localRankScore = localRank?.let {
                    LOCAL_RANK_WEIGHT /
                            (RRF_CONSTANT + it + 1)
                } ?: 0.0

                // Reciprocal-rank fusion for semantic results.
                val semanticRankScore = semanticRank?.let {
                    SEMANTIC_RANK_WEIGHT /
                            (RRF_CONSTANT + it + 1)
                } ?: 0.0

                // Actual Gemini similarity score.
                val similarity = similarityScores[song.id]
                    ?.coerceIn(0.0, 1.0)
                    ?: 0.0

                val similarityScore =
                    similarity * SIMILARITY_WEIGHT

                // Exact title and artist matches receive priority.
                val exactBoost = exactMatchScore(
                    song = song,
                    query = normalizedQuery
                )

                localRankScore +
                        semanticRankScore +
                        similarityScore +
                        exactBoost

            }.thenBy {
                it.title.lowercase(Locale.ROOT)
            }
        )
    }

    private fun exactMatchScore(
        song: Song,
        query: String
    ): Double {

        val title = normalize(song.title)
        val artist = normalize(song.artist)
        val album = normalize(song.album)
        val genre = normalize(song.genre)

        return when {
            title == query -> 1000.0
            artist == query -> 900.0
            album == query -> 800.0
            genre == query -> 700.0

            title.startsWith(query) -> 600.0
            artist.startsWith(query) -> 500.0

            title.contains(query) -> 400.0
            artist.contains(query) -> 300.0
            album.contains(query) -> 200.0
            genre.contains(query) -> 100.0

            else -> 0.0
        }
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
