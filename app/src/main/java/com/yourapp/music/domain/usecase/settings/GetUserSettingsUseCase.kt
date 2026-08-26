package com.yourapp.music.domain.usecase.settings

import com.yourapp.music.domain.model.UserSettings
import com.yourapp.music.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> = settingsRepository.observeSettings()
}
