package com.yourapp.music.presentation.auth.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false,

    // Email verification
    val canResendVerificationEmail: Boolean = false,
    val verificationEmailSent: Boolean = false,

    // Google account linking
    val showGoogleLinkDialog: Boolean = false,
    val pendingGoogleIdToken: String? = null,
    val pendingGoogleEmail: String? = null,
    val googleLinkPassword: String = ""
)