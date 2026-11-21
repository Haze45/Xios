package com.yourapp.music.domain.usecase.song

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

    suspend operator fun invoke(query: String): HybridSearchResult {
        val cleanedQuery = query.trim()

        if (cleanedQuery.isBlank()) {
            return HybridSearchResult(
                songs = emptyList(),
                semanticSearchSucceeded = false
            )
        }

        // First obtain the local Room-backed fuzzy matches.
        val localResults =
            searchSongsUseCase(cleanedQuery).first()

        // Then obtain Gemini semantic matches.
        val semanticResults = try {
            semanticSearchSongsUseCase(
                query = cleanedQuery,
                matchCount = 30
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Preserve local search if remote search is unavailable.
            return HybridSearchResult(
                songs = localResults,
                semanticSearchSucceeded = false
            )
        }

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
        semanticResults: List<Song>
    ): List<Song> {

        val normalizedQuery = normalize(query)

        val localRanks = localResults
            .mapIndexed { index, song -> song.id to index }
            .toMap()

        val semanticRanks = semanticResults
            .mapIndexed { index, song -> song.id to index }
            .toMap()

        // Merge both lists and keep only one copy of each song.
        val candidates = (localResults + semanticResults)
            .distinctBy { it.id }

        return candidates
            .sortedWith(
                compareByDescending<Song> { song ->

                    val localRank = localRanks[song.id]
                    val semanticRank =
                        semanticRanks[song.id]

                    val localScore = localRank?.let {
                        1.0 / (60 + it + 1)
                    } ?: 0.0

                    val semanticScore = semanticRank?.let {
                        1.0 / (60 + it + 1)
                    } ?: 0.0

                    val combinedRankScore =
                        (localScore + semanticScore) * 1000.0

                    val exactMatchBoost = exactMatchScore(
                        song = song,
                        query = normalizedQuery
                    )

                    combinedRankScore + exactMatchBoost
                }.thenBy {
                    it.title.lowercase(Locale.ROOT)
                }
            )
    }

    /**
     * Exact title and artist matches should outrank
     * approximate and semantic matches.
     */
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
