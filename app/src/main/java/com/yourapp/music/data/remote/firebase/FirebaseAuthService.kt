package com.yourapp.music.data.remote.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class EmailNotVerifiedException :
    Exception("Please verify your email address before logging in.")

@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun signIn(
        email: String,
        password: String
    ): FirebaseUser {

        val result = firebaseAuth
            .signInWithEmailAndPassword(
                email.trim(),
                password
            )
            .await()

        val user = result.user
            ?: throw IllegalStateException(
                "Login failed: no user returned"
            )

        // Get the latest verification status from Firebase.
        user.reload().await()

        val refreshedUser = firebaseAuth.currentUser
            ?: throw IllegalStateException(
                "Unable to refresh authenticated user"
            )

        if (!refreshedUser.isEmailVerified) {
            // Do not leave an unverified user signed in.
            firebaseAuth.signOut()
            throw EmailNotVerifiedException()
        }

        return refreshedUser
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): FirebaseUser {

        val result = firebaseAuth
            .createUserWithEmailAndPassword(
                email.trim(),
                password
            )
            .await()

        val user = result.user
            ?: throw IllegalStateException(
                "Registration failed: no user returned"
            )

        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
        ).await()

        // Firebase does not send this automatically.
        user.sendEmailVerification().await()

        return firebaseAuth.currentUser ?: user
    }

    suspend fun resendVerificationEmail() {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException(
                "No signed-in user found"
            )

        user.reload().await()

        val refreshedUser = firebaseAuth.currentUser
            ?: throw IllegalStateException(
                "Unable to refresh user"
            )

        if (refreshedUser.isEmailVerified) {
            throw IllegalStateException(
                "Email is already verified"
            )
        }

        refreshedUser.sendEmailVerification().await()
    }

    suspend fun resendVerificationEmail(
        email: String,
        password: String
    ) {
        var temporarySignInSucceeded = false

        try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = result.user
                ?: throw IllegalStateException(
                    "Unable to authenticate account"
                )

            temporarySignInSucceeded = true

            user.reload().await()

            val refreshedUser = firebaseAuth.currentUser
                ?: throw IllegalStateException(
                    "Unable to refresh user"
                )

            if (refreshedUser.isEmailVerified) {
                throw IllegalStateException(
                    "Email is already verified. You can log in."
                )
            }

            refreshedUser.sendEmailVerification().await()

        } finally {
            if (temporarySignInSucceeded) {
                firebaseAuth.signOut()
            }
        }
    }


    /**
     * Refresh and check the current user's verification status.
     */
    suspend fun isCurrentEmailVerified(): Boolean {
        val user = firebaseAuth.currentUser
            ?: return false

        user.reload().await()

        return firebaseAuth.currentUser?.isEmailVerified == true
    }

    /**
     * Update Firebase profile.
     */
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

    /**
     * Sign out current Firebase user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Get current Firebase user.
     */
    fun currentUser(): FirebaseUser? =
        firebaseAuth.currentUser

    /**
     * Observe Firebase authentication changes.
     */
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
            .signInWithEmailAndPassword(
                email.trim(),
                password
            )
            .await()

        val existingUser = signInResult.user
            ?: error(
                "Email/password sign-in returned no user"
            )

        existingUser.reload().await()

        if (!existingUser.isEmailVerified) {
            firebaseAuth.signOut()
            throw EmailNotVerifiedException()
        }

        val googleCredential =
            GoogleAuthProvider.getCredential(
                googleIdToken,
                null
            )

        val linkResult = try {
            existingUser
                .linkWithCredential(googleCredential)
                .await()
        } catch (exception: FirebaseAuthUserCollisionException) {
            throw exception
        }

        val linkedUser = linkResult.user
            ?: firebaseAuth.currentUser
            ?: error(
                "Account linking returned no user"
            )

        check(linkedUser.uid == existingUser.uid) {
            "Firebase UID changed unexpectedly during linking"
        }

        return linkedUser
    }

    /**
     * Normal Google sign-in.
     */
    suspend fun signInWithGoogleIdToken(
        idToken: String
    ): FirebaseUser {

        val credential =
            GoogleAuthProvider.getCredential(
                idToken,
                null
            )

        val result = firebaseAuth
            .signInWithCredential(credential)
            .await()

        return result.user
            ?: error(
                "Google sign-in failed: no user returned"
            )
    }
}