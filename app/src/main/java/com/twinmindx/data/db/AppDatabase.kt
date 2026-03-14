package com.twinmindx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        SummaryEntity::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptChunkDao(): TranscriptChunkDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transcript_chunks (
                        id TEXT NOT NULL PRIMARY KEY,
                        meetingId TEXT NOT NULL,
                        audioChunkId TEXT NOT NULL,
                        chunkIndex INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL,
                        FOREIGN KEY (audioChunkId) REFERENCES audio_chunks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transcript_chunks_audioChunkId ON transcript_chunks(audioChunkId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transcript_chunks_meetingId ON transcript_chunks(meetingId)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS summaries (
                        meetingId TEXT NOT NULL PRIMARY KEY,
                        title TEXT,
                        summary TEXT,
                        actionItems TEXT,
                        keyPoints TEXT,
                        streamingText TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        errorMessage TEXT,
                        createdAtMs INTEGER NOT NULL DEFAULT 0,
                        updatedAtMs INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
