package com.varaha.twinmindx.di

import com.varaha.twinmindx.data.api.MockSummaryService
import com.varaha.twinmindx.data.api.MockTranscriptionService
import com.varaha.twinmindx.data.api.SummaryService
import com.varaha.twinmindx.data.api.TranscriptionService
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
