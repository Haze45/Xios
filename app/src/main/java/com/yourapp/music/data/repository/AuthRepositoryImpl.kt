package com.yourapp.music.data.repository

import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.remote.firebase.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuthException
import com.yourapp.music.domain.model.User
import com.yourapp.music.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Resource<User> {
        return try {
            val user =
                firebaseAuthService.signIn(email, password)
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(
                authErrorKey(
                    e,
                    AuthOperation.LOGIN
                ), e
            )
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Resource<User> {
        return try {
            val user = firebaseAuthService.signUp(
                email,
                password,
                displayName
            )
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(
                authErrorKey(
                    e,
                    AuthOperation.REGISTER
                ), e
            )
        }
    }

    override suspend fun updateProfile(
        displayName: String?,
        photoUri: String?
    ): Resource<User> {
        return try {
            val uri = photoUri?.toUri()
            val user = firebaseAuthService.updateProfile(
                displayName,
                uri
            )
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(
                e.message
                    ?: "Couldn't update your profile. Please try again.",
                e
            )
        }
    }

    override fun logout() = firebaseAuthService.signOut()

    override fun getCurrentUser(): User? =
        firebaseAuthService.currentUser()?.toDomain()

    override fun observeAuthState(): Flow<User?> =
        firebaseAuthService.authStateFlow()
            .map { it?.toDomain() }

    override suspend fun loginWithGoogle(
        idToken: String
    ): Resource<User> {
        return try {
            val user =
                firebaseAuthService.signInWithGoogleIdToken(
                    idToken
                )
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(
                authErrorKey(
                    e,
                    AuthOperation.GOOGLE
                ), e
            )
        }
    }

    override suspend fun linkGoogleToExistingAccount(
        email: String,
        password: String,
        googleIdToken: String
    ): Resource<User> {
        return try {
            val user =
                firebaseAuthService.linkGoogleToEmailPasswordAccount(
                    email,
                    password,
                    googleIdToken
                )
            Resource.Success(user.toDomain())
        } catch (e: Exception) {
            Resource.Error(
                authErrorKey(
                    e,
                    AuthOperation.LOGIN
                ), e
            )
        }
    }

    private enum class AuthOperation { LOGIN, REGISTER, GOOGLE }

    private fun authErrorKey(
        error: Exception,
        operation: AuthOperation
    ): String {
        val code =
            (error as? FirebaseAuthException)?.errorCode.orEmpty()
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "auth_invalid_email"
            "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                if (operation == AuthOperation.LOGIN) "auth_invalid_credentials" else "auth_failed"

            "ERROR_EMAIL_ALREADY_IN_USE" ->
                if (operation == AuthOperation.GOOGLE) {
                    "auth_account_exists_different_provider"
                } else {
                    "auth_email_already_in_use"
                }

            "ERROR_WEAK_PASSWORD" -> "auth_weak_password"
            "ERROR_TOO_MANY_REQUESTS" -> "auth_too_many_requests"
            "ERROR_NETWORK_REQUEST_FAILED" -> "auth_network_error"
            "ERROR_USER_DISABLED" -> "auth_user_disabled"
            "ERROR_OPERATION_NOT_ALLOWED" -> "auth_provider_disabled"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "auth_account_exists_different_provider"
            else -> when (operation) {
                AuthOperation.LOGIN -> "auth_login_failed"
                AuthOperation.REGISTER -> "auth_register_failed"
                AuthOperation.GOOGLE -> "google_sign_in_failed"
            }
        }
    }
}
