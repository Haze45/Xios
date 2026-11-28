package com.yourapp.music.data.repository

import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuthException
import com.yourapp.music.core.util.Resource
import com.yourapp.music.data.mapper.toDomain
import com.yourapp.music.data.remote.firebase.EmailNotVerifiedException
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

    override suspend fun login(
        email: String,
        password: String
    ): Resource<User> = try {
        val user =
            firebaseAuthService.signIn(email, password)
        Resource.Success(user.toDomain())
    } catch (e: Exception) {
        Resource.Error(
            authErrorKey(e, AuthOperation.LOGIN),
            e
        )
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Resource<User> = try {
        val user = firebaseAuthService.signUp(
            email,
            password,
            displayName
        )
        Resource.Success(user.toDomain())
    } catch (e: Exception) {
        Resource.Error(
            authErrorKey(e, AuthOperation.REGISTER),
            e
        )
    }

    override suspend fun updateProfile(
        displayName: String?,
        photoUri: String?
    ): Resource<User> = try {
        val user = firebaseAuthService.updateProfile(
            displayName,
            photoUri?.toUri()
        )
        Resource.Success(user.toDomain())
    } catch (e: Exception) {
        Resource.Error(
            e.message
                ?: "Couldn't update your profile. Please try again.",
            e
        )
    }

    override fun logout() {
        firebaseAuthService.signOut()
    }

    override fun getCurrentUser(): User? =
        firebaseAuthService.currentUser()?.toDomain()

    override fun observeAuthState(): Flow<User?> =
        firebaseAuthService.authStateFlow().map { user ->
            user?.toDomain()
        }

    override suspend fun loginWithGoogle(
        idToken: String
    ): Resource<User> = try {
        val user =
            firebaseAuthService.signInWithGoogleIdToken(
                idToken
            )
        Resource.Success(user.toDomain())
    } catch (e: Exception) {
        Resource.Error(
            authErrorKey(e, AuthOperation.GOOGLE),
            e
        )
    }

    override suspend fun linkGoogleToExistingAccount(
        email: String,
        password: String,
        googleIdToken: String
    ): Resource<User> = try {
        val user =
            firebaseAuthService.linkGoogleToEmailPasswordAccount(
                email,
                password,
                googleIdToken
            )
        Resource.Success(user.toDomain())
    } catch (e: Exception) {
        Resource.Error(
            authErrorKey(e, AuthOperation.LINK),
            e
        )
    }

    override suspend fun resendVerificationEmail(): Resource<Boolean> =
        try {
            firebaseAuthService.resendVerificationEmail()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(
                e.message
                    ?: "Unable to send verification email",
                e
            )
        }

    override suspend fun resendVerificationEmail(
        email: String,
        password: String
    ): Resource<Boolean> = try {
        firebaseAuthService.resendVerificationEmail(
            email = email,
            password = password
        )

        Resource.Success(true)

    } catch (e: Exception) {
        val errorMessage = when {
            e.message?.contains(
                "already verified",
                ignoreCase = true
            ) == true -> "auth_email_already_verified"

            e is EmailNotVerifiedException ->
                "auth_email_not_verified"

            else -> authErrorKey(
                e,
                AuthOperation.LOGIN
            )
        }

        Resource.Error(errorMessage, e)
    }

    override suspend fun isCurrentEmailVerified(): Boolean =
        firebaseAuthService.isCurrentEmailVerified()

    private enum class AuthOperation {
        LOGIN,
        REGISTER,
        GOOGLE,
        LINK
    }

    private fun authErrorKey(
        error: Exception,
        operation: AuthOperation
    ): String {

        // Handle our own verification exception first.
        if (error is EmailNotVerifiedException) {
            return "auth_email_not_verified"
        }

        val code = (error as? FirebaseAuthException)
            ?.errorCode
            .orEmpty()

        return when (code) {
            "ERROR_INVALID_EMAIL" ->
                "auth_invalid_email"

            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" ->
                "auth_invalid_credentials"

            "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "auth_account_exists_different_provider"

            "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                "auth_google_credential_already_used"

            "ERROR_WEAK_PASSWORD" ->
                "auth_weak_password"

            "ERROR_TOO_MANY_REQUESTS" ->
                "auth_too_many_requests"

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "auth_network_error"

            "ERROR_USER_DISABLED" ->
                "auth_user_disabled"

            "ERROR_OPERATION_NOT_ALLOWED" ->
                "auth_provider_disabled"

            else -> when (operation) {
                AuthOperation.LOGIN ->
                    "auth_login_failed"

                AuthOperation.REGISTER ->
                    "auth_register_failed"

                AuthOperation.GOOGLE ->
                    "google_sign_in_failed"

                AuthOperation.LINK ->
                    "auth_link_failed"
            }
        }
    }
}
