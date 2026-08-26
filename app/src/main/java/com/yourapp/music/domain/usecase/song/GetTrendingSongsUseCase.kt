package com.yourapp.music.domain.usecase.song

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.SongRepository
import javax.inject.Inject

class GetTrendingSongsUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(): Resource<List<Song>> = songRepository.getTrendingSongs()
}
