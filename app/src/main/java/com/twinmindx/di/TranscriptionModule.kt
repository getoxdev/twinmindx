package com.twinmindx.di

import com.twinmindx.data.remote.transcription.TranscriptionService
import com.twinmindx.data.remote.transcription.TranscriptionServiceImpl
import com.twinmindx.data.remote.transcription.network.WhisperApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TranscriptionModule {

    @Provides
    @Singleton
    fun provideWhisperApiService(retrofit: Retrofit): WhisperApiService {
        return retrofit.create(WhisperApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTranscriptionService(
        transcriptionServiceImpl: TranscriptionServiceImpl
    ): TranscriptionService = transcriptionServiceImpl
}
