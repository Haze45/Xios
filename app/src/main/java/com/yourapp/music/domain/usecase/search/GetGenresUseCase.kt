package com.yourapp.music.domain.usecase.search

import com.yourapp.music.domain.model.Genre
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Not a stored/curated list — genres shown in "Browse by Genre" are
 * derived live from whatever `genre` tag actually exists on songs in the
 * catalog. Add a song with a genre no one's seen before (e.g. via the
 * generate_songs_sql.py script pulling it from the file's embedded tag)
 * and a new tile appears automatically the next time the song list
 * refreshes — no separate table to maintain.
 */
class GetGenresUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Genre>> = songRepository.getAllSongs().map { songs ->
        songs
            .map { it.genre }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .map { genreName -> Genre(id = genreName, name = genreName, colorHex = colorForGenre(genreName)) }
    }

    /**
     * Deterministic — the same genre name always gets the same color
     * (based on a hash of its text), rather than a color picked at
     * random or manually assigned in a database row somewhere.
     */
    private fun colorForGenre(genreName: String): String {
        val index = (genreName.lowercase().hashCode().let { if (it < 0) -it else it }) % PALETTE.size
        return PALETTE[index]
    }

    companion object {
        private val PALETTE = listOf(
            "#F7768E", "#7AA2F7", "#9ECE6A", "#E0AF68",
            "#BB9AF7", "#7DCFFF", "#FF9E64", "#73DACA",
            "#F7768E", "#B4F9F8", "#C0CAF5", "#FF757F"
        )
    }
}
