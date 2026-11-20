package com.yourapp.music.domain.usecase.song

import com.yourapp.music.domain.model.Song
import java.util.Locale

internal object SongSearchMatcher {

    private val ignoredWords = setOf(
        "a", "an", "the", "by", "for", "of", "and",
        "song", "songs", "track", "tracks", "music",
        "like", "similar", "please", "find", "play"
    )

    private data class Field(
        val text: String,
        val weight: Int
    )

    fun search(
        songs: List<Song>,
        rawQuery: String
    ): List<Song> {

        val query = normalize(rawQuery)

        if (query.isBlank()) return emptyList()

        val queryTokens = query.split(" ")
            .filter {
                it.isNotBlank() && it !in ignoredWords
            }
            .distinct()

        if (queryTokens.isEmpty()) return emptyList()

        return songs.mapNotNull { song ->

            val fields = listOf(
                Field(normalize(song.title), 100),
                Field(normalize(song.artist), 85),
                Field(normalize(song.album), 70),
                Field(normalize(song.genre), 55)
            ).filter {
                it.text.isNotBlank()
            }

            val score = scoreSong(
                query,
                queryTokens,
                fields
            )

            if (score <= 0) {
                null
            } else {
                song to score
            }
        }
            .sortedWith(
                compareByDescending<Pair<Song, Int>> {
                    it.second
                }.thenBy {
                    it.first.title.lowercase(Locale.ROOT)
                }
            )
            .take(60)
            .map {
                it.first
            }
    }

    private fun scoreSong(
        query: String,
        queryTokens: List<String>,
        fields: List<Field>
    ): Int {

        var total = 0

        // Strong boost for complete phrase matches.
        fields.forEach { field ->

            when {
                field.text == query ->
                    total += field.weight * 5

                field.text.startsWith(query) ->
                    total += field.weight * 3

                field.text.contains(query) ->
                    total += field.weight * 2
            }
        }

        // Every meaningful token contributes its best match.
        var matchedTokens = 0

        queryTokens.forEach { token ->

            val best = fields.maxOfOrNull { field ->
                tokenScore(
                    token,
                    field.text,
                    field.weight
                )
            } ?: 0

            if (best > 0) {
                matchedTokens++
                total += best
            }
        }

        // Reject weak matches for multi-word queries.
        val matchRatio =
            matchedTokens.toFloat() / queryTokens.size

        if (matchRatio < 0.5f) return 0

        // Prefer results matching every query word.
        if (matchedTokens == queryTokens.size) {
            total += 100
        } else {
            total = (total * matchRatio).toInt()
        }

        return total
    }

    private fun tokenScore(
        queryToken: String,
        fieldText: String,
        weight: Int
    ): Int {

        val words = fieldText.split(" ")
            .filter {
                it.isNotBlank()
            }

        if (words.any { it == queryToken }) {
            return weight * 3
        }

        if (words.any { it.startsWith(queryToken) }) {
            return weight * 2
        }

        if (words.any { it.contains(queryToken) }) {
            return weight
        }

        // Bounded typo tolerance.
        val maxDistance = when {
            queryToken.length >= 7 -> 2
            queryToken.length >= 4 -> 1
            else -> 0
        }

        if (maxDistance == 0) return 0

        val bestDistance = words.minOfOrNull {
            levenshteinDistance(
                queryToken,
                it,
                maxDistance
            )
        } ?: Int.MAX_VALUE

        return when (bestDistance) {
            1 -> weight * 2 / 3
            2 -> weight / 3
            else -> 0
        }
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    /**
     * Bounded Levenshtein distance.
     */
    private fun levenshteinDistance(
        left: String,
        right: String,
        limit: Int
    ): Int {

        if (kotlin.math.abs(left.length - right.length) > limit) {
            return limit + 1
        }

        var previous = IntArray(right.length + 1) {
            it
        }

        var current = IntArray(right.length + 1)

        for (i in 1..left.length) {

            current[0] = i
            var rowMinimum = current[0]

            for (j in 1..right.length) {

                val cost =
                    if (left[i - 1] == right[j - 1]) 0 else 1

                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )

                rowMinimum = minOf(
                    rowMinimum,
                    current[j]
                )
            }

            if (rowMinimum > limit) {
                return limit + 1
            }

            val temp = previous
            previous = current
            current = temp
        }

        return previous[right.length]
    }
}
