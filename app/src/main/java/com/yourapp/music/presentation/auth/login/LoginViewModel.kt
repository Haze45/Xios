package com.yourapp.music.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.AuthRepository
import com.yourapp.music.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val favoriteSyncManager: FavoriteSyncManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> =
        _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                errorMessage = null,
                canResendVerificationEmail = false,
                verificationEmailSent = false
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                errorMessage = null,
                canResendVerificationEmail = false,
                verificationEmailSent = false
            )
        }
    }

    fun setGoogleLoading(loading: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = loading,
                errorMessage = if (loading) null else it.errorMessage
            )
        }
    }

    fun onGoogleError(messageKey: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = messageKey
            )
        }
    }

    fun login() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    canResendVerificationEmail = false,
                    verificationEmailSent = false
                )
            }

            when (
                val result = loginUseCase(
                    state.email.trim(),
                    state.password
                )
            ) {
                is Resource.Success -> {
                    favoriteSyncManager.enqueueSync()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            canResendVerificationEmail = false
                        )
                    }
                }

                is Resource.Error -> {
                    val isUnverified =
                        result.message == "auth_email_not_verified"

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            canResendVerificationEmail = isUnverified
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * Resends the verification email using the credentials
     * currently entered on the login screen.
     */
    fun resendVerificationEmail() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "auth_credentials_required"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    verificationEmailSent = false
                )
            }

            when (
                val result =
                    authRepository.resendVerificationEmail(
                        email = state.email.trim(),
                        password = state.password
                    )
            ) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            verificationEmailSent = true,
                            canResendVerificationEmail = false,
                            errorMessage = null
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            canResendVerificationEmail = true
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    fun loginWithGoogle(
        idToken: String,
        googleEmail: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (
                val result =
                    authRepository.loginWithGoogle(idToken)
            ) {
                is Resource.Success -> {
                    favoriteSyncManager.enqueueSync()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true
                        )
                    }
                }

                is Resource.Error -> {
                    if (
                        result.message ==
                        "auth_account_exists_different_provider"
                    ) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                email = googleEmail,
                                pendingGoogleIdToken = idToken,
                                pendingGoogleEmail = googleEmail,
                                googleLinkPassword = "",
                                showGoogleLinkDialog = true,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    fun onGoogleLinkPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                googleLinkPassword = value,
                errorMessage = null
            )
        }
    }

    fun dismissGoogleLinkDialog() {
        _uiState.update {
            it.copy(
                showGoogleLinkDialog = false,
                pendingGoogleIdToken = null,
                pendingGoogleEmail = null,
                googleLinkPassword = "",
                errorMessage = null
            )
        }
    }

    fun confirmGoogleAccountLink() {
        val state = _uiState.value
        val token = state.pendingGoogleIdToken ?: return
        val email = state.pendingGoogleEmail ?: return

        if (state.googleLinkPassword.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "auth_password_required"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (
                val result =
                    authRepository.linkGoogleToExistingAccount(
                        email = email,
                        password = state.googleLinkPassword,
                        googleIdToken = token
                    )
            ) {
                is Resource.Success -> {
                    favoriteSyncManager.enqueueSync()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            showGoogleLinkDialog = false,
                            pendingGoogleIdToken = null,
                            pendingGoogleEmail = null,
                            googleLinkPassword = ""
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }
}
