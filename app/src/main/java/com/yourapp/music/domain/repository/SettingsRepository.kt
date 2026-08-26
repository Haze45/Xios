package com.yourapp.music.domain.repository

import com.yourapp.music.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
}
