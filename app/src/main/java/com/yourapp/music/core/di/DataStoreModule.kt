package com.yourapp.music.core.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.yourapp.music.core.util.Constants
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = Constants.DATASTORE_SETTINGS)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context) =
        context.settingsDataStore
}
