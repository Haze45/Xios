package com.yourapp.music.core.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for triggering favorite sync. Call enqueueSync()
 * right after AddFavoriteUseCase/RemoveFavoriteUseCase, on successful
 * login, and optionally on network-reconnect.
 */
@Singleton
class FavoriteSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork("favorite_sync", ExistingWorkPolicy.KEEP, request)
    }
}
