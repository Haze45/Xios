package com.yourapp.music.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.core.util.Resource
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
    private val syncProfileUseCase: SyncProfileUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> =
        _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update {
        it.copy(
            displayName = value,
            errorMessage = null
        )
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(
            email = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(
            password = value,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update {
            it.copy(
                confirmPassword = value,
                errorMessage = null
            )
        }

    fun register() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }
            when (val result = registerUseCase(
                state.email,
                state.password,
                state.confirmPassword,
                state.displayName
            )) {
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

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "error_generic"
                    )
                }

                is Resource.Loading -> Unit
            }
        }
    }
}
