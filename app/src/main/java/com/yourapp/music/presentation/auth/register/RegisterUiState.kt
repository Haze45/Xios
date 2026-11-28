package com.yourapp.music.presentation.auth.register

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccessful: Boolean = false,
    // Email verification
    val isVerificationEmailSent: Boolean = false,
    // Google account linking
    val showGoogleLinkDialog: Boolean = false,
    val pendingGoogleIdToken: String? = null,
    val pendingGoogleEmail: String? = null,
    val googleLinkPassword: String = ""
)