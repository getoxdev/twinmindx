package com.twinmindx.di

import com.twinmindx.data.repository.RecordingRepositoryImpl
import com.twinmindx.data.repository.SummaryRepositoryImpl
import com.twinmindx.data.repository.TranscriptionRepositoryImpl
import com.twinmindx.domain.repository.RecordingRepository
import com.twinmindx.domain.repository.SummaryRepository
import com.twinmindx.domain.repository.TranscriptionRepository
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
    abstract fun bindRecordingRepository(
        impl: RecordingRepositoryImpl
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(
        impl: TranscriptionRepositoryImpl
    ): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSummaryRepository(
        impl: SummaryRepositoryImpl
    ): SummaryRepository
}
