package com.yourapp.music.core.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yourapp.music.core.download.EncryptedAwareDataSourceFactory
import com.yourapp.music.core.download.SongEncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        encryptionManager: SongEncryptionManager
    ): ExoPlayer {
        val dataSourceFactory =
            EncryptedAwareDataSourceFactory(
                context = context,
                encryptionManager = encryptionManager
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(
            dataSourceFactory
        )

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
}
