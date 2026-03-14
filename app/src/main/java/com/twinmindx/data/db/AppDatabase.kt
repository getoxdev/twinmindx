package com.twinmindx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.twinmindx.data.db.dao.AudioChunkDao
import com.twinmindx.data.db.dao.MeetingDao
import com.twinmindx.data.db.entity.AudioChunkEntity
import com.twinmindx.data.db.entity.MeetingEntity

@Database(
    entities = [
        MeetingEntity::class,
        AudioChunkEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun audioChunkDao(): AudioChunkDao
}
