package com.yourapp.music.domain.usecase.settings

import com.yourapp.music.domain.model.UserSettings
import com.yourapp.music.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: UserSettings) = settingsRepository.updateSettings(settings)
}
