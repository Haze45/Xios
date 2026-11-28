package com.yourapp.music.domain.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): Resource<User>

    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Resource<User>

    suspend fun updateProfile(
        displayName: String?,
        photoUri: String?
    ): Resource<User>

    fun logout()

    fun getCurrentUser(): User?

    fun observeAuthState(): Flow<User?>

    suspend fun loginWithGoogle(
        idToken: String
    ): Resource<User>

    suspend fun linkGoogleToExistingAccount(
        email: String,
        password: String,
        googleIdToken: String
    ): Resource<User>

    suspend fun resendVerificationEmail(): Resource<Boolean>
    suspend fun resendVerificationEmail(
        email: String,
        password: String
    ): Resource<Boolean>

    suspend fun isCurrentEmailVerified(): Boolean
}