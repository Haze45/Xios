package com.yourapp.music.data.repository

import com.yourapp.music.data.local.datastore.SettingsDataStore
import com.yourapp.music.domain.model.UserSettings
import com.yourapp.music.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> = settingsDataStore.userSettings

    override suspend fun updateSettings(settings: UserSettings) =
        settingsDataStore.updateSettings(settings)
}
