package com.yourapp.music.data.repository

import android.net.Uri
import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.remote.firebase.FirebaseAuthService
import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val user = firebaseAuthService.signIn(email, password)
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed. Please try again.", e)
        }
    }

    override suspend fun register(email: String, password: String, displayName: String): Resource<User> {
        return try {
            val user = firebaseAuthService.signUp(email, password, displayName)
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed. Please try again.", e)
        }
    }

    override suspend fun updateProfile(displayName: String?, photoUri: String?): Resource<User> {
        return try {
            val uri = photoUri?.let { Uri.parse(it) }
            val user = firebaseAuthService.updateProfile(displayName, uri)
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Couldn't update your profile. Please try again.", e)
        }
    }

    override fun logout() = firebaseAuthService.signOut()

    override fun getCurrentUser(): User? = firebaseAuthService.currentUser()?.toDomain()

    override fun observeAuthState(): Flow<User?> =
        firebaseAuthService.authStateFlow().map { it?.toDomain() }
}
