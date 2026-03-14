package com.varaha.twinmindx.data.db

import androidx.room.TypeConverter
import com.varaha.twinmindx.data.db.entity.ChunkStatus
import com.varaha.twinmindx.data.db.entity.MeetingStatus
import com.varaha.twinmindx.data.db.entity.SummaryStatus

class Converters {

    @TypeConverter
    fun fromMeetingStatus(status: MeetingStatus): String = status.name

    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus = MeetingStatus.valueOf(value)

    @TypeConverter
    fun fromChunkStatus(status: ChunkStatus): String = status.name

    @TypeConverter
    fun toChunkStatus(value: String): ChunkStatus = ChunkStatus.valueOf(value)

    @TypeConverter
    fun fromSummaryStatus(status: SummaryStatus): String = status.name

    @TypeConverter
    fun toSummaryStatus(value: String): SummaryStatus = SummaryStatus.valueOf(value)
}
