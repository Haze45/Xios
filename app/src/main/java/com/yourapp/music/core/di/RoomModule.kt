package com.yourapp.music.core.di

import android.content.Context
import androidx.room.Room
import com.yourapp.music.core.util.Constants
import com.yourapp.music.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSongDao(db: AppDatabase) = db.songDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase) =
        db.playlistDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase) =
        db.favoriteDao()

    @Provides
    fun provideArtistDao(db: AppDatabase) =
        db.artistDao()

    @Provides
    fun provideRecentSearchDao(db: AppDatabase) =
        db.recentSearchDao()

    @Provides
    fun provideDownloadDao(db: AppDatabase) =
        db.downloadDao()

    @Provides
    fun provideListeningHistoryDao(db: AppDatabase) =
        db.listeningHistoryDao()
}
