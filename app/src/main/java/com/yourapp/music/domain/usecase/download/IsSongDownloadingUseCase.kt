package com.yourapp.music.domain.usecase.download

import com.yourapp.music.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsSongDownloadingUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(songId: String): Flow<Boolean> = downloadRepository.isDownloading(songId)
}
