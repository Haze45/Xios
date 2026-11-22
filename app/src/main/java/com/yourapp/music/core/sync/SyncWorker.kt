package com.yourapp.music.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.remote.firebase.FirebaseAuthService
import com.yourapp.music.domain.repository.ListeningHistoryRepository
import com.yourapp.music.domain.usecase.auth.GetCurrentUserUseCase
import com.yourapp.music.domain.usecase.favorite.SyncFavoritesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val firebaseAuthService: FirebaseAuthService,
    private val listeningHistoryRepository: ListeningHistoryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val user = getCurrentUserUseCase()
                ?: return Result.success()

            val firebaseUser =
                firebaseAuthService.currentUser()
                    ?: return Result.success()

            // Ensure the Firebase identity matches the app's current user.
            if (firebaseUser.uid != user.uid) {
                return Result.retry()
            }

            val idToken = firebaseUser.getIdToken(false)
                .await()
                .token
                ?: return Result.retry()

            // Sync favorites first.
            when (syncFavoritesUseCase(user.uid)) {
                is Resource.Success -> Unit
                is Resource.Error -> return Result.retry()
                is Resource.Loading -> return Result.retry()
            }

            // Then sync listening history.
            listeningHistoryRepository.syncPendingHistory(
                userId = user.uid,
                firebaseIdToken = idToken
            )

            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
