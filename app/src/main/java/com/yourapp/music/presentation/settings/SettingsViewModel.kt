package com.yourapp.music.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.core.sync.FavoriteSyncManager
import com.yourapp.music.domain.model.StreamingQuality
import com.yourapp.music.domain.usecase.auth.LogoutUseCase
import com.yourapp.music.domain.usecase.settings.GetUserSettingsUseCase
import com.yourapp.music.domain.usecase.settings.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getUserSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun onStreamingQualityChange(quality: StreamingQuality) = persist { it.copy(streamingQuality = quality) }

    fun onVolumeNormalizationToggle(enabled: Boolean) =
        persist { it.copy(volumeNormalizationEnabled = enabled) }

    fun onPlaybackNotificationsToggle(enabled: Boolean) =
        persist { it.copy(playbackNotificationsEnabled = enabled) }

    fun onGeneralNotificationsToggle(enabled: Boolean) =
        persist { it.copy(generalNotificationsEnabled = enabled) }

    fun onGranularNotificationsToggle(enabled: Boolean) =
        persist { it.copy(granularNotificationsEnabled = enabled) }

    private fun persist(transform: (com.yourapp.music.domain.model.UserSettings) -> com.yourapp.music.domain.model.UserSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = updated) } // optimistic UI update
        viewModelScope.launch { updateSettingsUseCase(updated) }
    }

    fun logout() = logoutUseCase()
}
