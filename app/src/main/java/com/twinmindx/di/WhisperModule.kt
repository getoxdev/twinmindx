package com.twinmindx.di

import com.twinmindx.data.remote.transcription.network.WhisperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * DI module for Whisper transcription API dependencies.
 * Uses the same Retrofit instance from SummaryModule.
 */
@Module
@InstallIn(SingletonComponent::class)
object WhisperModule {

    @Provides
    @Singleton
    fun provideWhisperApiService(retrofit: Retrofit): WhisperApiService {
        return retrofit.create(WhisperApiService::class.java)
    }
}
