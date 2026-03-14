package com.twinmindx.di

import com.twinmindx.data.transcription.MockTranscriptionService
import com.twinmindx.data.transcription.TranscriptionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TranscriptionModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionService(
        mock: MockTranscriptionService
    ): TranscriptionService
}
