package com.yourapp.music.domain.usecase.auth

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Resource<User> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.Error("Email and password cannot be empty")
        }
        if (!email.contains("@")) {
            return Resource.Error("Enter a valid email address")
        }
        return authRepository.login(email.trim(), password)
    }
}
