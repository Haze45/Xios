package com.yourapp.music.domain.usecase.song

import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class SearchSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> =
        songRepository.getAllSongs().map { songs ->
            SongSearchMatcher.search(songs, query)
        }
}
