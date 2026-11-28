package com.yourapp.music.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.core.util.Resource
import com.yourapp.music.domain.repository.AuthRepository
import com.yourapp.music.domain.usecase.auth.RegisterUseCase
import com.yourapp.music.domain.usecase.profile.SyncProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val favoriteSyncManager: FavoriteSyncManager,
    private val syncProfileUseCase: SyncProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(RegisterUiState())

    val uiState: StateFlow<RegisterUiState> =
        _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                displayName = value,
                errorMessage = null
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                errorMessage = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                errorMessage = null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                errorMessage = null
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

    /**
     * Registers an email/password account.
     * The verification email is sent by FirebaseAuthService.
     * The user is then signed out until their email is verified.
     */
    fun register() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isVerificationEmailSent = false
                )
            }

            when (
                val result = registerUseCase(
                    state.email.trim(),
                    state.password,
                    state.confirmPassword,
                    state.displayName
                )
            ) {
                is Resource.Success -> {
                    // Create/sync the user's profile while the newly
                    // created Firebase user is still available.
                    syncProfileUseCase(
                        result.data.uid,
                        followingCount = 0,
                        playlistCount = 0
                    )

                    // Do not keep an unverified user signed in.
                    authRepository.logout()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegisterSuccessful = false,
                            isVerificationEmailSent = true,
                            errorMessage = null,
                            password = "",
                            confirmPassword = ""
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

    /**
     * Same Google authentication and collision handling
     * as the Login screen.
     */
    fun registerWithGoogle(
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

                    syncProfileUseCase(
                        result.data.uid,
                        followingCount = 0,
                        playlistCount = 0
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegisterSuccessful = true
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
                it.copy(errorMessage = "auth_password_required")
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

                    syncProfileUseCase(
                        result.data.uid,
                        followingCount = 0,
                        playlistCount = 0
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegisterSuccessful = true,
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
