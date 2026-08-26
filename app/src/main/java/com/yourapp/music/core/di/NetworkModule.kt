package com.yourapp.music.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Reserved for Ktor/Retrofit client providers if you add
    // any REST endpoints outside of Supabase's built-in client.
}
