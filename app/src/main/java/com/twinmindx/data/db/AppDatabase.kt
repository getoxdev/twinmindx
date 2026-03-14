package com.twinmindx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.dao.MeetingDao
import com.twinmindx.data.db.dao.SummaryDao
import com.twinmindx.data.db.dao.TranscriptChunkDao
import com.twinmindx.data.db.entity.AudioChunkEntity
import com.twinmindx.data.db.entity.MeetingEntity
import com.twinmindx.data.db.entity.SummaryEntity
import com.twinmindx.data.db.entity.TranscriptChunkEntity

@Database(
    entities = [
        MeetingEntity::class,
        AudioChunkEntity::class,
        TranscriptChunkEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptChunkDao(): TranscriptChunkDao
    abstract fun summaryDao(): SummaryDao
}
