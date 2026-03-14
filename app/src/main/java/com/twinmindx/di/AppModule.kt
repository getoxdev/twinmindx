package com.twinmindx.di

import com.twinmindx.data.api.MockSummaryService
import com.twinmindx.data.api.MockTranscriptionService
import com.twinmindx.data.api.SummaryService
import com.twinmindx.data.api.TranscriptionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionService(impl: MockTranscriptionService): TranscriptionService

    @Binds
    @Singleton
    abstract fun bindSummaryService(impl: MockSummaryService): SummaryService
}
