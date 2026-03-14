package com.varaha.twinmindx.di

import android.content.Context
import androidx.room.Room
import com.varaha.twinmindx.data.db.AppDatabase
import com.varaha.twinmindx.data.db.dao.AudioChunkDao
import com.varaha.twinmindx.data.db.dao.MeetingDao
import com.varaha.twinmindx.data.db.dao.SummaryDao
import com.varaha.twinmindx.data.db.dao.TranscriptChunkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "twinmindx_db"
        ).build()
    }

    @Provides
    fun provideMeetingDao(db: AppDatabase): MeetingDao = db.meetingDao()

    @Provides
    fun provideAudioChunkDao(db: AppDatabase): AudioChunkDao = db.audioChunkDao()

    @Provides
    fun provideTranscriptChunkDao(db: AppDatabase): TranscriptChunkDao = db.transcriptChunkDao()

    @Provides
    fun provideSummaryDao(db: AppDatabase): SummaryDao = db.summaryDao()
}
