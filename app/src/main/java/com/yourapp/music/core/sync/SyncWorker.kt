package com.yourapp.music.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.SyncFavoritesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = getCurrentUserUseCase()
            ?: return Result.success() // not logged in, nothing to sync

        return when (syncFavoritesUseCase(user.uid)) {
            is Resource.Success -> Result.success()
            is Resource.Error -> Result.retry()
            is Resource.Loading -> Result.retry()
        }
    }
}
