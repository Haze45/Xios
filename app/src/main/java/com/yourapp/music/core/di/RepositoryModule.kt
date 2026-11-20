package com.yourapp.music.core.di

import com.yourapp.music.data.repository.AuthRepositoryImpl
import com.yourapp.music.data.repository.ArtistRepositoryImpl
import com.yourapp.music.data.repository.DownloadRepositoryImpl
import com.yourapp.music.data.repository.FavoriteRepositoryImpl
import com.yourapp.music.data.repository.PlaylistRepositoryImpl
import com.yourapp.music.data.repository.ProfileRepositoryImpl
import com.yourapp.music.data.repository.SearchRepositoryImpl
import com.yourapp.music.data.repository.SettingsRepositoryImpl
import com.yourapp.music.data.repository.SongRepositoryImpl
import com.yourapp.music.domain.repository.AuthRepository
import com.yourapp.music.domain.repository.ArtistRepository
import com.yourapp.music.domain.repository.DownloadRepository
import com.yourapp.music.domain.repository.FavoriteRepository
import com.yourapp.music.domain.repository.PlaylistRepository
import com.yourapp.music.domain.repository.ProfileRepository
import com.yourapp.music.domain.repository.SearchRepository
import com.yourapp.music.domain.repository.SettingsRepository
import com.yourapp.music.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindArtistRepository(impl: ArtistRepositoryImpl): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
}
