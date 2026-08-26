package com.yourapp.music.domain.usecase.auth

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ): Resource<User> {
        if (displayName.isBlank()) return Resource.Error("Name cannot be empty")
        if (!email.contains("@")) return Resource.Error("Enter a valid email address")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        if (password != confirmPassword) return Resource.Error("Passwords do not match")

        return authRepository.register(email.trim(), password, displayName.trim())
    }
}
