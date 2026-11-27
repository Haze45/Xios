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
                    errorMessage = null
                )
            }
            when (val result = loginUseCase(
                state.email,
                state.password
            )) {
                is Resource.Success -> {
                    favoriteSyncManager.enqueueSync() // pull down favorites saved on other devices
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true
                        )
                    }
                }

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }

                is Resource.Loading -> _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
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

            when (val result =
                authRepository.loginWithGoogle(idToken)) {
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
                    if (result.message == "auth_account_exists_different_provider") {
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

                is Resource.Loading -> _uiState.update {
                    it.copy(isLoading = false)
                }
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
                googleLinkPassword = ""
            )
        }
    }

    fun confirmGoogleAccountLink() {
        val state = _uiState.value
        val token = state.pendingGoogleIdToken ?: return
        val googleEmail = state.pendingGoogleEmail ?: return
        if (state.googleLinkPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "auth_password_required") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }
            when (val result =
                authRepository.linkGoogleToExistingAccount(
                    email = googleEmail,
                    password = state.googleLinkPassword,
                    googleIdToken = token
                )) {
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

                is Resource.Loading -> _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }
}
