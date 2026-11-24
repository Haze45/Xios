package com.yourapp.music.domain.usecase.download

import com.yourapp.music.domain.model.DownloadProgress
import com.yourapp.music.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDownloadProgressUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadProgress>> =
        downloadRepository.observeDownloadProgress()
}
