package com.yourapp.music.domain.usecase.song

import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.GetFavoritesUseCase
import com.yourapp.music.domain.usecase.search.SemanticSearchSongsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetPersonalizedRecommendationsUseCase @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val semanticSearchSongsUseCase: SemanticSearchSongsUseCase
) {

    suspend operator fun invoke(): List<Song> {
        val user =
            getCurrentUserUseCase() ?: return emptyList()

        // Read the user's latest favorites from Room.
        val favoriteSongs =
            getFavoritesUseCase(user.uid).first()

        if (favoriteSongs.isEmpty()) {
            return emptyList()
        }

        val favoriteIds = favoriteSongs
            .map { it.id }
            .toSet()

        // Use a small selection to construct a focused preference query.
        val preferenceDescription = favoriteSongs
            .take(5)
            .joinToString(separator = "; ") { song ->
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

        val query = (
                "Find songs similar to these favorite songs: " +
                        preferenceDescription
                ).take(300)

        // Reuse the existing Gemini semantic-search endpoint.
        val semanticMatches = semanticSearchSongsUseCase(
            query = query,
            matchCount = 30
        )

        // Exclude favorites, prioritize higher similarity scores,
        // and return at most ten recommendations.
        return semanticMatches
            .filter { match ->
                match.song.id !in favoriteIds
            }
            .distinctBy { it.song.id }
            .sortedByDescending { match ->
                match.similarity ?: Double.NEGATIVE_INFINITY
            }
            .take(10)
            .map { it.song }
    }
}
