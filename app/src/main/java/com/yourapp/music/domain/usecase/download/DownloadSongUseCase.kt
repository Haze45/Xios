package com.yourapp.music.domain.usecase.download

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.Song
import com.yourapp.music.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadSongUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(song: Song): Resource<Unit> = downloadRepository.downloadSong(song)
}
