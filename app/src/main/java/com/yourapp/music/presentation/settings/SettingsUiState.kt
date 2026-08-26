package com.yourapp.music.presentation.settings

import com.yourapp.music.domain.model.UserSettings

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)
