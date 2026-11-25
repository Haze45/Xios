package com.yourapp.music.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.music.domain.model.UserSettings
import com.yourapp.music.domain.usecase.settings.GetUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    getUserSettingsUseCase: GetUserSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<UserSettings> =
        getUserSettingsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = UserSettings()
            )
}
