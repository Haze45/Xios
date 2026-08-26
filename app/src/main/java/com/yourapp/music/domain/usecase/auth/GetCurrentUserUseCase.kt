package com.yourapp.music.domain.usecase.auth

import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): User? = authRepository.getCurrentUser()

    fun observe(): Flow<User?> = authRepository.observeAuthState()
}
