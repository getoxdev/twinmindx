package com.twinmindx.data.db

import androidx.room.TypeConverter
import com.twinmindx.data.db.entity.ChunkStatus
import com.twinmindx.data.db.entity.MeetingStatus

class Converters {

    @TypeConverter
    fun fromMeetingStatus(status: MeetingStatus): String = status.name

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus = MeetingStatus.valueOf(value)

    @TypeConverter
    fun fromChunkStatus(status: ChunkStatus): String = status.name

    @TypeConverter
    fun toChunkStatus(value: String): ChunkStatus = ChunkStatus.valueOf(value)
}
