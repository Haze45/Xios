package com.yourapp.music.data.remote.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around FirebaseAuth. Keeps all Firebase SDK calls
 * isolated here so AuthRepositoryImpl stays testable and Firebase-agnostic.
 */
@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun signIn(
        email: String,
        password: String
    ): FirebaseUser {
        val result =
            firebaseAuth.signInWithEmailAndPassword(
                email,
                password
            ).await()
        return result.user
            ?: error("Login failed: no user returned")
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): FirebaseUser {
        val result =
            firebaseAuth.createUserWithEmailAndPassword(
                email,
                password
            ).await()
        val user = result.user
            ?: error("Registration failed: no user returned")
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName).build()
        ).await()
        return user
    }

    suspend fun updateProfile(
        displayName: String?,
        photoUri: Uri?
    ): FirebaseUser {
        val user = firebaseAuth.currentUser
            ?: error("No signed-in user")
        val request =
            UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUri?.let { setPhotoUri(it) }
            }.build()
        user.updateProfile(request).await()
        user.reload().await()
        return firebaseAuth.currentUser ?: user
    }

    fun signOut() = firebaseAuth.signOut()

    fun currentUser(): FirebaseUser? =
        firebaseAuth.currentUser

    /** Emits the current user whenever Firebase's auth state changes (login/logout). */
    fun authStateFlow(): Flow<FirebaseUser?> =
        callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    trySend(auth.currentUser)
                }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(
                    listener
                )
            }
        }

    suspend fun linkGoogleToEmailPasswordAccount(
        email: String,
        password: String,
        googleIdToken: String
    ): FirebaseUser {
        val signInResult = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()
        val existingUser = signInResult.user
            ?: error("Email/password sign-in returned no user")

        val googleCredential = GoogleAuthProvider.getCredential(
            googleIdToken,
            null
        )
        val linkResult = existingUser
            .linkWithCredential(googleCredential)
            .await()

        return linkResult.user
            ?: firebaseAuth.currentUser
            ?: error("Google account linking returned no user")
    }

    suspend fun signInWithGoogleIdToken(
        idToken: String
    ): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        val result = firebaseAuth.signInWithCredential(
            credential
        ).await()

        return result.user
            ?: error("Google sign-in failed: no user returned")
    }
}
