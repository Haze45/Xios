package com.yourapp.music.domain.usecase.song

import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getAllSongs()
}
